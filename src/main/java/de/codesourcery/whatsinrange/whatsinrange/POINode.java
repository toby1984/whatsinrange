package de.codesourcery.whatsinrange.whatsinrange;

import java.time.Duration;

public class POINode 
{
    public long nodeId; // DB primary key
    public long osmNodeId;
    public String omsmNodeName;
    public NodeType nodeType;
    public String hvvName;
    public Duration timeToCentralStation;
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
                + timeToCentralStation + ", osmNodeLocation=" + osmNodeLocation + "]";
    }    
}
