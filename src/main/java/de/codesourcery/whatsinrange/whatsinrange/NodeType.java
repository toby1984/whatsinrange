package de.codesourcery.whatsinrange.whatsinrange;

public enum NodeType { 
    BUS_STOP("bus_stop"),
    SUBWAY_STATION("subway_station"),
    LIGHT_RAIL_STATION("light_rail_station"),
    UNKNOWN("unknown");
    
    private final String dbIdentifier;
    
    private NodeType(String dbIdentifier) {
        this.dbIdentifier = dbIdentifier;
    }

    public String getDbIdentifier() {
        return dbIdentifier;
    }
    
    public static NodeType fromDbIdentifier(String id) {
        switch( id ) {
            case "bus_stop": return NodeType.BUS_STOP;
            case "subway_station": return NodeType.SUBWAY_STATION;
            case "light_rail_station": return NodeType.LIGHT_RAIL_STATION;
            case "unknown": return NodeType.UNKNOWN;
            default:
                throw new IllegalArgumentException("Unhandled node db identifier: '"+id+"'");
        }
    }
}