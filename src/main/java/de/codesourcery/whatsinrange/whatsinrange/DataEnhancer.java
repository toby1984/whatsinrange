package de.codesourcery.whatsinrange.whatsinrange;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataEnhancer implements AutoCloseable, DisposableBean
{
    private static final Logger LOG = LogManager.getLogger( HVVScraper.class );

    // @GuardedBy(POOL_LOCK)    
    private final ThreadPoolExecutor threadPool;

    private final Object POOL_LOCK = new Object();

    // @GuardedBy(POOL_LOCK)    
    private boolean shutdown=false;

    @Autowired
    private IMapDataStorage dataStorage;

    private final ConcurrentHashMap<Thread,HVVScraper> scrapersByThread = new ConcurrentHashMap<>();

    public DataEnhancer() 
    {
        final BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(300);
        threadPool = new ThreadPoolExecutor( 20,20,60,TimeUnit.SECONDS,workQueue );
    }

    public void run() 
    {
        final List<POINode> list = dataStorage.getAllNodesWithNoTravelTime();
        LOG.info("run(): Querying HVV for "+list.size()+" POIs");        
        if ( list.size() > 0 ) 
        {
            final Object SUBMIT_LOCK = new Object();
            final CountDownLatch latch = new CountDownLatch(list.size());
            for (int i = 0; i < list.size() ; i++) 
            {
                final POINode node = list.get(i);
                
                synchronized( POOL_LOCK ) {
                    if ( shutdown ) {
                        break;
                    }
                }
                
                final Runnable runnable = () -> 
                {
                    try 
                    {
                        HVVScraper scraper=null;
                        synchronized( POOL_LOCK ) {
                            if ( ! shutdown ) {
                                scraper = getScraper();
                            }
                        }
                        if ( scraper != null) 
                        {
                            final long start = System.currentTimeMillis();
                            final SearchResult result = scraper.query( node );
                            node.timeToCentralStation = result.travelTime;
                            dataStorage.saveOrUpdate( Collections.singleton( node ) );
                            final float processed = list.size() - latch.getCount();
                            final float percentage = 100*( processed / list.size() );
                            final long finish = System.currentTimeMillis();
                            LOG.info("run(): Got result for "+node.hvvName+" - "+percentage+"% done ("+processed+" of "+list.size()+") in "+(finish-start)+" ms");
                        } else {
                            LOG.info("run(): Shutdown called, not retrieving result for "+node);
                        }
                    } 
                    catch(Exception e) {
                        LOG.error("run(): Failed to get result for "+node,e);
                    }
                    finally 
                    {
                        synchronized(SUBMIT_LOCK) {
                            SUBMIT_LOCK.notifyAll();
                        }
                        latch.countDown();
                    }
                };
                
                while( true ) 
                {
                    try 
                    {
                        synchronized(POOL_LOCK) 
                        {
                            if ( shutdown ) {
                                LOG.info("run(): shutdown called, aborting.");
                            } else {
                                threadPool.submit( runnable );
                            }
                            break;
                        }
                    } 
                    catch(RejectedExecutionException e) 
                    {
                        LOG.info("run(): Queue full,waiting...");
                        synchronized( SUBMIT_LOCK ) 
                        {
                            try {
                                SUBMIT_LOCK.wait(3*1000);
                                LOG.info("run(): Queue empty,trying to submit again");
                            } catch (InterruptedException e1) {
                                // nothing to see here
                            }
                        }
                    }
                }
            };

            while(true) 
            {
                LOG.info("run(): Waiting for all queries to finish");
                try {
                    latch.await();
                    LOG.info("run(): All queries finished");
                    break;
                } catch (InterruptedException e) {
                    LOG.error("run(): Caught,e");
                }
            }
        }
    }

    private HVVScraper getScraper() 
    {
        final Thread key = Thread.currentThread();
        HVVScraper result = scrapersByThread.get( key );
        if ( result == null ) {
            result = new HVVScraper();
            scrapersByThread.put(key,result);
        }
        return result;
    }
    @Override
    public void close() throws Exception 
    {
        synchronized(POOL_LOCK) 
        {
            if ( ! shutdown )
            {
                shutdown = true;
                try 
                {
                    LOG.info("close(): Terminating thread pool...");
                    threadPool.shutdown();
                    if ( ! threadPool.awaitTermination( 60 ,  TimeUnit.SECONDS ) ) {
                        LOG.error("close(): Failed to terminate thread pool?");
                    }
                } 
                finally 
                {
                    for ( HVVScraper scraper : scrapersByThread.values() ) 
                    {
                        try {
                            scraper.close();
                        } catch(Exception e) {
                            LOG.error("close(): Failed to terminate scraper "+scraper,e);
                        }
                    }
                }
            } else {
                LOG.info("close(): Shutdown already called");
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        close();
    }
}
