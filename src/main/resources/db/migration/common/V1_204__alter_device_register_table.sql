DROP SEQUENCE IF EXISTS der_seq;

ALTER TABLE device_register DROP CONSTRAINT device_register_pk;

ALTER TABLE device_register RENAME TO node_register;

CREATE SEQUENCE nod_seq START WITH 10000;
