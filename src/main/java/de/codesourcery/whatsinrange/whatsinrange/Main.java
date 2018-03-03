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
            final String map = "/hamburg.osm";
            final InputStream in = App.class.getResourceAsStream( map );            
            ctx.getBean(App.class).importData(in);
        }
    }
}
