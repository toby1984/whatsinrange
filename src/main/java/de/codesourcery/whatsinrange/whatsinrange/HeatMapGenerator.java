package de.codesourcery.whatsinrange.whatsinrange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GlobalPosition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HeatMapGenerator {

    private static final double MAX_WEIGHT = 10d;
    
    @Autowired
    private IMapDataStorage datastorage;

    private ThreadPoolExecutor pool;

    protected static final class HeatmapData {
        public double longitude;
        public double latitude;
        public double weight;
    }

    public HeatMapGenerator() {

        final BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(300);
        final ThreadFactory threadFactory = new ThreadFactory() {

            private final AtomicLong id = new AtomicLong(0);
            @Override
            public Thread newThread(Runnable r) {
                final Thread t = new Thread(r);
                t.setDaemon( true );
                t.setName("thread-"+id.incrementAndGet());
                return t;
            }
        };
        pool = new ThreadPoolExecutor(20,20,60,TimeUnit.SECONDS,workQueue,threadFactory,new ThreadPoolExecutor.CallerRunsPolicy() );
    }

    public void generateHeatMap(Coordinates min,Coordinates max, int stepsX,int stepsY,OutputStream out) throws IOException 
    {
        try ( PrintWriter writer = new PrintWriter( out ) ) 
        {
            double dx = Math.abs(max.longitude - min.longitude) / (double) stepsX;
            double dy = Math.abs(max.latitude - min.latitude) / (double) stepsY;
            long totalPoints = stepsX*stepsY;
            final AtomicLong processedPoints = new AtomicLong();
            final AtomicLong lastPrintAtProcessedPointsCount = new AtomicLong();
            final float fivePercent = totalPoints * 0.05f;
            final long start = System.currentTimeMillis();

            double longitude = min.longitude; // Längengrad , X axis
            double latitude = min.latitude; // Breitengrad, Y axis

            final Object TIME_LOCK = new Object();
            final CountDownLatch latch = new CountDownLatch( (int) totalPoints );
            final List<HeatmapData> list = new ArrayList<HeatmapData>( (int) totalPoints );
            for ( int x = 0 ; x < stepsX ; longitude += dx,x++) 
            {
                latitude = min.latitude;
                for ( int y = 0 ; y < stepsY ; latitude += dy , y++ ) 
                {
                    final double finalLat = latitude;
                    final double finalLong = longitude;

                    final Runnable r = new Runnable() 
                    {
                        @Override
                        public void run() 
                        {
                            try 
                            {
                                final POINode closest = datastorage.findClosestNode( finalLong, finalLat , true ).get();
                                final Coordinates coords = new Coordinates();
                                coords.longitude = finalLong;
                                coords.latitude = finalLat;                                    
                                double distance = calcDistance(closest.osmNodeLocation,coords);
                                float minutes = closest.timeToCentralStation.getSeconds()/60f;
                                minutes += (distance/60f); // + distance * 1 m/s
                                
                                final HeatmapData data = new HeatmapData();
                                data.latitude = finalLat;
                                data.longitude = finalLong;
                                data.weight = minutes;                                    
                                synchronized( list ) 
                                {
                                    list.add( data );
                                }
                            } 
                            finally 
                            {
                                synchronized( TIME_LOCK ) 
                                {
                                    long processed = processedPoints.incrementAndGet();
                                    if ( (processed-lastPrintAtProcessedPointsCount.get()) >= fivePercent ) 
                                    {
                                        final long now = System.currentTimeMillis();
                                        final float elapsed = now - start;
                                        float pointsPerSecond = processed/(elapsed/1000f);
                                        float secondsRemaining = (totalPoints - processed) / pointsPerSecond;
                                        final float processedPercentage = 100f * ( processed/(float)totalPoints);
                                        System.out.println("Processed: "+processedPercentage+"% ("+processedPoints+" of "+totalPoints+", "+secondsRemaining+" seconds remaining");
                                        lastPrintAtProcessedPointsCount.set( processed );
                                    }                          
                                }
                                latch.countDown();
                            }
                        }
                    };
                    pool.submit( r );
                }
            }
            
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.interrupted();
            }
            
            boolean scale = true;
            double scaleFactor = 1d;
            double minweight = Float.MAX_VALUE;
            float maxweight = Float.MIN_VALUE;            
            if ( scale ) 
            {
                for ( HeatmapData data : list ) 
                {
                    if ( data.weight > maxweight ) {
                        maxweight = (float) data.weight;
                    }
                    if ( data.weight < minweight ) {
                        minweight = (float)data.weight;
                    }
                }
                scaleFactor = MAX_WEIGHT/(maxweight-minweight); // 0..10
            }
            
            writer.println("window.heatMapData = ["); // GLOBAL VARIABLE....BAD...
            boolean first = true;
            for ( HeatmapData data : list ) 
            {
                double weight = data.weight;
                if ( scale ) {
                    weight = (weight - minweight) * scaleFactor;
                } 
//                if ( data.weight > 30 ) {
//                    weight = MAX_WEIGHT;
//                }
                if ( first ) {
                    writer.print( "{location: new google.maps.LatLng("+data.latitude+", "+data.longitude+"), weight: "+weight+"}");
                    first = false;
                } else {
                    writer.print( ",\n{location: new google.maps.LatLng("+data.latitude+", "+data.longitude+"), weight: "+weight+"}");
                }
            }
            writer.println("];");
            
            /*
             * write markers
             */
            
            writer.println("window.calcMarkerData = function(theMap) {\n"
                    + " return [ "); // GLOBAL VARIABLE....BAD...
            
            first = true;
            for ( POINode node : datastorage.getAllNodesWithNoTravelTime() ) {
                final String title = "Name: "+node.hvvName;
                final Coordinates data = node.osmNodeLocation;
                if ( first ) {
                    writer.print( "new google.maps.Marker({position: new google.maps.LatLng("+data.latitude+", "+data.longitude+"), map: theMap, title: \""+title+"\"})");
                    first = false;
                } else {
                    writer.print( ",\nnew google.maps.Marker({position: new google.maps.LatLng("+data.latitude+", "+data.longitude+"), map: theMap, title: \""+title+"\"})");
                }            	
            }
            writer.println("];\n};");            
        }
    }

    private double calcDistance(Coordinates point1,Coordinates point2) 
    {
        final GeodeticCalculator geoCalc = new GeodeticCalculator();
        Ellipsoid reference = Ellipsoid.WGS84;  
        GlobalPosition pointA = new GlobalPosition(point1.latitude, point1.longitude, 0.0); // Point A
        GlobalPosition userPos = new GlobalPosition(point2.latitude, point2.longitude, 0.0); // Point B
        return geoCalc.calculateGeodeticCurve(reference, userPos, pointA).getEllipsoidalDistance();        
    }
}
