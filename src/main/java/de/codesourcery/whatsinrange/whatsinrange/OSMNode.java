package de.codesourcery.whatsinrange.whatsinrange;

import java.util.Map;
import java.util.stream.Collectors;

public final class OSMNode 
{
    public long nodeId;
    public Coordinates coordinates;
    public Map<String,String> tags;

    public OSMNode(long nodeId,Coordinates coordinates,Map<String,String> tags) {
        this.nodeId = nodeId;
        this.coordinates = coordinates;
        this.tags = tags;
    }

    public String name() {
        return tags.get("name");
    }

    public NodeType nodeType() {
        return getNodeType(this.tags);
    }

    public boolean containsTagValue(String value) {
        return containsTagValue(value,this.tags);
    }

    public static boolean containsTagValue(String value,Map<String,String> tags) {
        for ( Map.Entry<String,String> entry : tags.entrySet() ) {
            if ( entry.getValue().contains( value ) ) {
                return true;
            }
        }
        return false;
    }        

    public boolean isBusStop() {
        return isBusStop( this.tags );
    }
    
    public boolean isTrainStation() {
    	return isTrainStation( this.tags );
    }

    public static boolean isBusStop(Map<String,String> tags) {
    	return contains("bus","yes",tags) && 
    	        ( contains("public_transport","stop_position",tags) || 
    		      contains("public_transport","platform",tags) ||  
    		      contains("highway","bus_stop",tags)  
    		   )
    		   && 
    		   tags.containsKey("name");
    }

    public static boolean isSubwayHalt(Map<String,String> tags) {
        return contains("subway","yes",tags) &&
             contains("public_transport","stop_position",tags) &&
             tags.containsKey("name");
    }
    
    public static boolean isTrainStation(Map<String,String> tags) {
        return contains("train","yes",tags) &&
             contains("railway","halt",tags) &&
             tags.containsKey("name");
    }
    
    private static boolean contains(String key,String value,Map<String,String> tags) {
    	return value.equals( tags.get(key ) );
    }
    
    public static boolean isLightRailHalt(Map<String,String> tags) {
        return contains("light_rail","yes",tags) &&
                contains("public_transport","stop_position",tags) &&
                tags.containsKey("name");
    }        

    public static NodeType getNodeType(Map<String,String> tags) 
    {
        if ( isBusStop(tags) ) {
            return NodeType.BUS_STOP;
        } else if ( isSubwayHalt( tags ) ) {
            return NodeType.SUBWAY_STATION;
        } else if ( isLightRailHalt( tags ) ) {
            return NodeType.LIGHT_RAIL_STATION;
        } else if ( isTrainStation( tags ) ) {
        	return NodeType.TRAIN_STATION;
        }
        return NodeType.UNKNOWN;
    }        

    public void mergeTags(Map<String,String> other) 
    {
        for ( Map.Entry<String,String> entry : other.entrySet() ) {
            String key = entry.getKey();
            String value = entry.getValue();
            String existingValue = tags.get( key );
            if ( existingValue == null ) {
                tags.put(key,value);
            } else if ( ! value.equals( existingValue ) ) {
                tags.put(key,existingValue+" "+value);
            }
        }
    }

    public String getTagString() {
        if ( tags.isEmpty() ) {
            return "";
        }
        return tags.entrySet().stream().map( e -> e.getKey()+"="+e.getValue() ).collect( Collectors.joining( " " ) );
    }
}