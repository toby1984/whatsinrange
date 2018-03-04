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
            app.importData(in);
            app.enhanceData();
        }
    }
}
