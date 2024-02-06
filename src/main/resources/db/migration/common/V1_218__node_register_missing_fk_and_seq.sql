-- v47
-- changes were not kept update to date for the node_register table so its being done now with the new
-- changes for the table in v47.
CREATE SEQUENCE node_seq START WITH 10000;

ALTER TABLE node_register
  ADD CONSTRAINT node_register_created_by_fk
    FOREIGN KEY (created_by) REFERENCES user_account (usr_id);

ALTER TABLE node_register
  ADD CONSTRAINT node_register_modified_by_fk
    FOREIGN KEY (last_modified_by) REFERENCES user_account (usr_id);

ALTER TABLE node_register
  ALTER COLUMN hostname SET NOT NULL;
ALTER TABLE node_register
  ALTER COLUMN ip_address SET NOT NULL;
ALTER TABLE node_register
  ALTER COLUMN mac_address SET NOT NULL;

ALTER SEQUENCE node_seq CACHE 20 RESTART WITH 50000;
