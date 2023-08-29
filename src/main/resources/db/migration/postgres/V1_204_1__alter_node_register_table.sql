ALTER TABLE node_register ALTER COLUMN node_id SET NOT NULL;

ALTER TABLE node_register ADD COLUMN type CHARACTER VARYING;

CREATE UNIQUE INDEX node_register_pk ON node_register(node_id);
ALTER TABLE node_register ADD PRIMARY KEY USING INDEX node_register_pk;
