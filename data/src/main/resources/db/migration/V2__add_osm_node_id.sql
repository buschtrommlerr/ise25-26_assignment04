ALTER TABLE pos ADD COLUMN IF NOT EXISTS osm_node_id BIGINT;
-- Ensure uniqueness when provided
CREATE UNIQUE INDEX IF NOT EXISTS ux_pos_osm_node_id ON pos(osm_node_id);

