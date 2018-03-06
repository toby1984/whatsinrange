package de.codesourcery.whatsinrange.whatsinrange;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Coordinates 
{
    private static final String FLOAT_REGEX = "[+\\-]?[0-9]*(\\.[0-9]+)?";
    private static final Pattern POINT_REGEX = Pattern.compile("POINT\\(("+FLOAT_REGEX+") ("+FLOAT_REGEX+")\\)");

    public double longitude;
    public double latitude;

    public Coordinates() {
    }
    public Coordinates(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }
    
    public static final class LongBuilder 
    {
        private final double longitude;
        
        public LongBuilder(double longitude) {
            this.longitude = longitude;
        }
        
        public Coordinates latitude(double latitude) {
            return new Coordinates(longitude,latitude);
        }
    }
    
    public static final class LatBuilder 
    {
        private final double latitude;
        
        public LatBuilder(double latitude) {
            this.latitude = latitude;
        }
        
        public Coordinates longitude(double longitude) {
            return new Coordinates(longitude,latitude);
        }
    }    

    public static LongBuilder lng(double longitude) {
        return new LongBuilder(longitude);
    }
    
    public static LatBuilder lat(double latitude) {
        return new LatBuilder(latitude);
    }    
    
    public Coordinates(Coordinates coordinates) {
        this.longitude = coordinates.longitude;
        this.latitude = coordinates.latitude;
    }

    public static Coordinates of(double longitude,double latitude) {
        return new Coordinates(longitude,latitude);
    }
    
    public static Coordinates fromPostGISPoint(String text) 
    {
        final Matcher m = POINT_REGEX.matcher( text );
        if ( ! m.matches() ) {
          throw new IllegalArgumentException("Not a valid PostGIS POINT: "+text);    
        }
        final double longitude = Double.parseDouble( m.group(1) );
        final double latitude = Double.parseDouble( m.group(3) );
        final Coordinates result = new Coordinates(longitude,latitude);
        return result;
    }
    
    @Override
    public String toString() {
        return "lon="+longitude+" | lat="+latitude;
    }
    public Coordinates createCopy() {
        return new Coordinates(this);
    }
}