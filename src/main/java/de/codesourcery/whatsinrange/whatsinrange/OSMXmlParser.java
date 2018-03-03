package de.codesourcery.whatsinrange.whatsinrange;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.springframework.stereotype.Component;

/**
 * Hello world!
 *
 */
@Component("osmXmlParser")
public class OSMXmlParser 
{
    // node
    private boolean inNode;
    private long nodeId;
    private double longitude;
    private double latitude;
    
    public static final class NamedLocations 
    {
        public final NodeType type;
        
        public Map<Long,OSMNode> locations = new HashMap<>();
        public Map<String,OSMNode> locationsByName = new HashMap<>();
        
        public NamedLocations(NodeType type) {
            this.type = type;
        }
        
        public int size() {
            return locationsByName.size();
        }
        
        public OSMNode getNodeById(Long id) {
            return locations.get( id );
        }
        
    }
    
    private final Map<NodeType,NamedLocations> namedLocations = new HashMap<>();
    
    public OSMXmlParser()
    {
        for ( NodeType t : NodeType.values() ) {
            if ( t != NodeType.UNKNOWN ) {
                namedLocations.put( t , new NamedLocations(t) );
            }
        }
    }

    // tag
    private Map<String,String> tags = new HashMap<>();
    
    public void parse(InputStream in) throws FileNotFoundException, XMLStreamException 
    {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLStreamReader parser  = factory.createXMLStreamReader( in );
        System.out.println("Started parsing...");
        long eventCount = 0;
        long nodeCount = 0;
        long tagCount = 0;
        int event;
        while( (event = parser.next() ) != XMLStreamConstants.END_DOCUMENT  ) 
        {
            eventCount++;
            switch(event) 
            {
                case XMLStreamConstants.START_ELEMENT:
                    String tagName = parser.getLocalName();
                    if ( "node".equals( tagName ) ) {
                        nodeCount++;
                        inNode = true;
                        if ( ! tags.isEmpty() ) {
                            tags = new HashMap<>();
                        }
                        final int count = parser.getAttributeCount();
                        for ( int i = 0 ; i < count ; i++ ) 
                        {
                            String attr = parser.getAttributeLocalName( i );
                            if ( "lat".equals( attr ) ) {
                                latitude = Double.parseDouble( parser.getAttributeValue( i ) );
                            } else if ( "lon".equals( attr ) ) {
                                longitude = Double.parseDouble( parser.getAttributeValue( i ) );
                            } else if ( "id".equals( attr ) ) {
                                nodeId = Long.parseLong( parser.getAttributeValue( i ) );
                            }
                        }
                    } 
                    else if ( inNode && "tag".equals( tagName ) ) 
                    {
                        tagCount++;

                        String tagKey = null;
                        String tagValue = null;
                        final int count = parser.getAttributeCount();
                        for ( int i = 0 ; i < count ; i++ ) 
                        {
                            String attr = parser.getAttributeLocalName( i );
                            if ( "k".equals( attr ) ) {
                                tagKey = parser.getAttributeValue( i );
                            } else if ( "v".equals( attr ) ) {
                                tagValue = parser.getAttributeValue( i );
                            }
                        }
                        if ( tagKey != null && tagValue != null ) 
                        {
                            String value = tags.get( tagKey );
                            if ( value == null ) {
                                tags.put(tagKey,tagValue);
                            } else if ( ! value.equals( tagValue ) ) {
                                tags.put(tagKey,value+" "+tagValue);
                            }
                        }
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    tagName = parser.getLocalName();
                    if ( inNode && "node".equals( tagName ) ) 
                    {
                        NodeType type = OSMNode.getNodeType( tags );
                        if ( type != NodeType.UNKNOWN )
                        {
                            final NamedLocations locations = getNamedLocations( type );
                            final Long key = Long.valueOf( nodeId );
                            OSMNode existing = locations.getNodeById( key );
                            if ( existing != null ) {
                                existing.mergeTags( this.tags );
                            } else {
                                existing = new OSMNode(nodeId,new Coordinates(longitude,latitude),tags);
                                final String nodeName = existing.name();
                                if ( nodeName != null ) {
                                    final String lowerName = existing.name().toLowerCase();                                
                                    final Map<String,OSMNode> byNameMap = locations.locationsByName;
                                    final OSMNode existingStop = byNameMap.get( lowerName );
                                    if ( existingStop == null ) {
                                        locations.locations.put( key, existing );
                                        byNameMap.put( lowerName, existing );
                                    } else {
                                        existingStop.mergeTags( tags );
                                    }
                                }
                            }
                            this.tags = new HashMap<>();
                        }
                        inNode = false;
                        nodeId = -1;
                        longitude = latitude = 0;
                    }
                    break;
                default:
            }
        }
        System.out.println("Finished parsing (events="+eventCount+", nodes="+nodeCount+", tags="+tagCount+")");
    }
    
    public NamedLocations getNamedLocations(NodeType type) {
        NamedLocations result = namedLocations.get( type );
        if ( result == null ) {
            throw new RuntimeException("No named locations for "+type);
        }
        return result;
    }
}
