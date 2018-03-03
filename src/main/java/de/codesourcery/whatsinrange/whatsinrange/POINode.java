package de.codesourcery.whatsinrange.whatsinrange;

public class POINode 
{
    /*
    node_id bigint PRIMARY KEY NOT NULL,
    osm_node_id bigint UNIQUE NOT NULL,
    osm_node_name text NOT NULL,
    node_type node_type NOT NULL,
    hvv_name text DEFAULT NULL,
//    minutes_to_central_station float DEFAULT NULL,
    osm_location geography(POINT)
     */
    public long nodeId; // DB primary key
    public long osmNodeId;
    public String omsmNodeName;
    public NodeType nodeType;
    public String hvvName;
    public float minutesToCentralStation;
    public Coordinates osmNodeLocation;
    
    public POINode() {
    }
    
    public POINode(OSMNode node) {
        this.osmNodeId = node.nodeId;
        this.omsmNodeName = node.name();
        this.nodeType = node.nodeType();
        this.hvvName = node.name();
        this.osmNodeLocation = node.coordinates.createCopy();
    }
    
    public boolean isPersistent() {
        return nodeId != 0;
    }

    @Override
    public String toString() {
        return "POINode [nodeId=" + nodeId + ", osmNodeId=" + osmNodeId + ", omsmNodeName=" + omsmNodeName
                + ", nodeType=" + nodeType + ", hvvName=" + hvvName + ", minutesToCentralStation="
                + minutesToCentralStation + ", osmNodeLocation=" + osmNodeLocation + "]";
    }    
}
