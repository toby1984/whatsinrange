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
            // app.importData(in);
            app.enhanceData();
            
//            final FileOutputStream out = new FileOutputStream("/home/tobi/oxygen_workspace/whatsinrange/src/main/resources/data.js");
//            
//            //   <bounds minlat="53.4390000" minlon="9.6982000" maxlat="53.6656000" maxlon="10.3141000"/>
//            Coordinates min = new Coordinates(9.6982000,53.4390000);
//            Coordinates max = new Coordinates(10.3141000,53.6656000);
//            
//            app.generateHeatMap(min,max,100,out);
        }
    }
}
