package de.codesourcery.whatsinrange.whatsinrange;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("mapDataStorage")
public class MapDataStorage extends JdbcTemplate implements IMapDataStorage 
{
    public static final String POI_NODES = "poi_nodes";
    /*
CREATE TABLE poi_nodes (
  node_id bigint PRIMARY KEY NOT NULL,
  osm_node_id bigint UNIQUE NOT NULL,
  osm_node_name text NOT NULL,
  node_type node_type NOT NULL, 
  hvv_name text DEFAULT NULL,
  minutes_to_central_station float DEFAULT NULL,
  osm_location geography(POINT)
);
     */
    
    private static final RowMapper<POINode> ROW_MAPPER = (rs,rowNum) -> 
    {
        POINode n = new POINode();
        n.nodeId = rs.getLong("node_id");
        n.osmNodeId = rs.getLong("osm_node_id");
        n.omsmNodeName = rs.getString("osm_node_name");
        n.nodeType = NodeType.fromDbIdentifier( rs.getString("node_type" ) );
        n.hvvName = rs.getString("hvv_name");
        n.minutesToCentralStation = rs.getFloat("minutes_to_central_station");
        n.osmNodeLocation = Coordinates.fromPostGISPoint( rs.getString( "location" ) );
        return n;
    };
    
    @Transactional
    @Override
    public void saveOrUpdate(Collection<POINode> nodes) 
    {
        final List<POINode> toUpdate = nodes.stream().filter( n -> n.isPersistent() ).collect( Collectors.toList() );
        final List<POINode> toInsert = nodes.stream().filter( n -> ! n.isPersistent() ).collect( Collectors.toList() );

        // INSERT
        if ( ! toInsert.isEmpty() ) 
        {
            execute( new ConnectionCallback<Void>() 
            {
                @Override
                public Void doInConnection(Connection con) throws SQLException, DataAccessException 
                {
                    final String sql = "INSERT INTO "+POI_NODES+" (osm_node_id,osm_node_name,node_type,hvv_name,minutes_to_central_station,osm_location) VALUES (?,?,?::node_type,?,?,ST_SetSRID(ST_MakePoint(?, ?), 4326))";
                    try ( PreparedStatement stmt = con.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS) ) 
                    {
                        for ( POINode n : toInsert ) {
                            stmt.setLong(1, n.osmNodeId );
                            stmt.setString(2,n.omsmNodeName);
                            stmt.setString(3,n.nodeType.getDbIdentifier());
                            stmt.setString(4,n.hvvName);
                            stmt.setFloat(5,n.minutesToCentralStation);
                            stmt.setDouble(6, n.osmNodeLocation.longitude);
                            stmt.setDouble(7, n.osmNodeLocation.latitude);
                            stmt.addBatch();
                        }                        
                        stmt.executeBatch();
                        try ( ResultSet rs = stmt.getGeneratedKeys() ) 
                        {
                            for ( POINode n : toInsert ) 
                            {
                                if ( ! rs.next() ) {
                                    throw new SQLException("INSERT didn't return a key?");
                                }
                                n.nodeId = rs.getLong(1);
                            }
                        }                        
                    }
                    return null;
                }
            });
        }
        
        // UPDATE
        if ( ! toUpdate.isEmpty() ) 
        {
            execute( new ConnectionCallback<Void>() 
            {
                @Override
                public Void doInConnection(Connection con) throws SQLException, DataAccessException {

                    final String sql = "UPDATE "+POI_NODES+" SET osm_node_id=?,osm_node_name=?,node_type=?::nodeType,hvv_name=?,minutes_to_central_station=?,osm_location=ST_SetSRID(ST_MakePoint(?, ?), 4326) WHERE node_id=?";
                    try ( PreparedStatement stmt = con.prepareStatement(sql) ) {
                        for ( POINode n : toInsert ) {
                            stmt.setLong(1, n.osmNodeId );
                            stmt.setString(2,n.omsmNodeName);
                            stmt.setString(3,n.nodeType.getDbIdentifier());
                            stmt.setString(4,n.hvvName);
                            stmt.setFloat(5,n.minutesToCentralStation);
                            stmt.setDouble(6, n.osmNodeLocation.longitude);
                            stmt.setDouble(7, n.osmNodeLocation.latitude);
                            stmt.setLong(8, n.nodeId);
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                    return null;
                }
            });
        }        
    }
    
    @Transactional
    @Override
    public Optional<POINode> getNode(long id) 
    {
        final String sql = "SELECT node_id,osm_node_id,osm_node_name,node_type,hvv_name,minutes_to_central_station,ST_AsText(osm_location) AS location FROM "+POI_NODES+" WHERE node_id="+id;
        return Optional.ofNullable( queryForObject(sql,ROW_MAPPER) );
    }
    
    @Transactional
    @Override
    public Optional<POINode> findClosestNode(Coordinates coordinates) 
    {
        /*
  node_id bigint PRIMARY KEY NOT NULL,
  osm_node_id bigint UNIQUE NOT NULL,
  osm_node_name text NOT NULL,
  node_type node_type NOT NULL, 
  hvv_name text DEFAULT NULL,
  minutes_to_central_station float DEFAULT NULL,
  osm_location geography(POINT)         
         */
        final String sql = "SELECT node_id,osm_node_id,osm_node_name,node_type,hvv_name,minutes_to_central_station,ST_AsText(osm_location) AS location,"
                + "ST_DISTANCE(osm_location,ST_SetSRID(ST_MakePoint(?, ?), 4326)) AS distance FROM "+POI_NODES+" ORDER BY distance ASC LIMIT 1";
        final List<POINode> result = query(sql,new Object[] {coordinates.longitude,coordinates.latitude}, ROW_MAPPER );
        if ( result.size() > 1 ) {
            throw new RuntimeException("Result set size > 1 ?");
        }
        return result.isEmpty() ? Optional.empty() : Optional.of( result.get(0) );
    }

    @Autowired
    @Override
    public void setDataSource(DataSource dataSource) {
        super.setDataSource(dataSource);
    }
}