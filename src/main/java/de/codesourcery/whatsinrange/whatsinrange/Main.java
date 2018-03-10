package de.codesourcery.whatsinrange.whatsinrange;

import java.io.FileOutputStream;
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
            long time1 = System.currentTimeMillis();
//             app.importData(in);
             long time2 = System.currentTimeMillis();
             System.out.println("Import took "+(time2-time1)+" ms");
            
//            app.enhanceData( new ConsoleChoiceCallback() );
            
            final FileOutputStream out = new FileOutputStream("/home/tobi/oxygen_workspace/whatsinrange/src/main/resources/data.js");

            Coordinates min = Coordinates.lat(53.5313000).longitude(9.2972000);
            Coordinates max = Coordinates.lat(53.9698000).longitude(10.6924000);
            
            app.generateHeatMap(min,max,200,out);
        }
    }
}
