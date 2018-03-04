package de.codesourcery.whatsinrange.whatsinrange;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IMapDataStorage {

    public void saveOrUpdate(Collection<POINode> nodes);
    
    public Optional<POINode> findClosestNode(Coordinates coordinates);

    public Optional<POINode> getNode(long id);
    
    public List<POINode> getAllNodesWithNoTravelTime();
}
