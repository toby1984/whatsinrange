DROP DATABASE IF EXISTS whatsinrange;

CREATE DATABASE whatsinrange;

\c whatsinrange

BEGIN;

CREATE EXTENSION postgis;

CREATE TYPE node_type AS ENUM ('bus_stop', 'subway_station','light_rail_station','unknown');

DROP SEQUENCE IF EXISTS poi_nodes_seq;

CREATE SEQUENCE poi_nodes_seq;

CREATE TABLE poi_nodes (
  node_id bigint PRIMARY KEY DEFAULT nextval('poi_nodes_seq'),
  osm_node_id bigint UNIQUE NOT NULL,
  osm_node_name text NOT NULL,
  node_type node_type NOT NULL, 
  hvv_name text DEFAULT NULL,
  minutes_to_central_station float DEFAULT NULL,
  osm_location geography(POINT)
);

CREATE INDEX poi_nodes_location ON poi_nodes USING GIST(osm_location);
CREATE INDEX poi_nodes_node_type ON poi_nodes(node_type);

COMMIT;
