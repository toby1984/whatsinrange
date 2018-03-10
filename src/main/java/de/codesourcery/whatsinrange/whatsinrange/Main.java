package de.codesourcery.whatsinrange.whatsinrange;

import java.io.InputStream;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main 
{
    public static void main( String[] args ) throws Exception
    {
        try ( ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("/spring.xml")  )
        {
            // do stuff
//            final String map = "/map.osm";
            final String map = "/hamburg.osm";
            final InputStream in = App.class.getResourceAsStream( map );            
            final App app = ctx.getBean(App.class);
            // app.importData(in);
            
            app.enhanceData( new ConsoleChoiceCallback() );
            
//            final FileOutputStream out = new FileOutputStream("/home/tobi/oxygen_workspace/whatsinrange/src/main/resources/data.js");
//
//              // <bounds minlat="53.5313000" minlon="9.2972000" maxlat="53.9698000" maxlon="10.6924000"/>
//            Coordinates min = Coordinates.lat(53.5313000).longitude(9.2972000);
//            Coordinates max = Coordinates.lat(53.9698000).longitude(10.6924000);
//            
//            app.generateHeatMap(min,max,100,out);
        }
    }
}
