ALTER TABLE node_register
  ALTER COLUMN node_id SET NOT NULL;

ALTER TABLE node_register
  ADD COLUMN type CHARACTER VARYING;

ALTER TABLE node_register
  ADD CONSTRAINT node_register_pk PRIMARY KEY (node_id);
