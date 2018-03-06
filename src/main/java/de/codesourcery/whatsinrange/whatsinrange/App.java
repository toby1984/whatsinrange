package de.codesourcery.whatsinrange.whatsinrange;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hello world!
 *
 */
@Component("app")
public class App 
{
    @Autowired
    private IMapDataStorage storage;
    
    @Autowired
    private DataEnhancer dataEnhancer;
    
    @Autowired
    private HeatMapGenerator heatmapGenerator;

    @Transactional
    public void importData(InputStream in) throws FileNotFoundException, XMLStreamException 
    {
        if ( in == null ) {
            throw new RuntimeException("Input stream must not be NULL");
        }
        final OSMXmlParser parser = new OSMXmlParser();
        parser.parse(in);
//        for ( NodeType t : NodeType.values() ) {
//            if ( t != NodeType.UNKNOWN ) 
//            {
//                parser.getNamedLocations(t).locationsByName.values().forEach( n -> 
//                {
//                    System.out.println("Node "+n.nodeId+" | "+n.name()+" | "+n.nodeType()+" | "+n.coordinates+" | "+n.getTagString());
//                });
//            }
//        }

        for ( NodeType t : NodeType.values() ) {
            if ( t != NodeType.UNKNOWN ) {
                System.out.println( t.toString()+": "+parser.getNamedLocations(t).size());
            }
        }

        System.out.println("Persisting data...");
        for ( NodeType t : NodeType.values() ) {
            if ( t != NodeType.UNKNOWN ) {
                storage.saveOrUpdate( parser.getNamedLocations(t).locationsByName.values().stream().map( POINode::new ).collect( Collectors.toList() ) );
            }
        }
        System.out.println("Finished persisting data.");
    }    
    
    public void enhanceData() 
    {
        dataEnhancer.run();
    }
    
    public void generateHeatMap(Coordinates min,Coordinates max, int steps,OutputStream out) throws IOException 
    {
        heatmapGenerator.generateHeatMap(min,max,(int) (steps*1.5f),steps,out);
    }
}
