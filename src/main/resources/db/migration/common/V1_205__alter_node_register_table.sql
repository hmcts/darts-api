ALTER TABLE node_register
  RENAME COLUMN type TO device_type;

ALTER TABLE node_register
  DROP COLUMN der_id;

ALTER TABLE node_register
  DROP CONSTRAINT device_register_courtroom_fk;

ALTER TABLE node_register
  ADD CONSTRAINT node_register_courtroom_fk
    FOREIGN KEY (ctr_id) REFERENCES courtroom (ctr_id);
