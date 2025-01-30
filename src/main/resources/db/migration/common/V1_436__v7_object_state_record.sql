CREATE TABLE IF NOT EXISTS revinfo
(
    rev        INT4 NOT NULL,
    revtstmp   INT8,
    CONSTRAINT revinfo_pkey PRIMARY KEY(rev)
);

ALTER TABLE revinfo
    ADD COLUMN audit_user INTEGER;

ALTER TABLE revinfo
    ADD CONSTRAINT revinfo_audit_user_fk
        FOREIGN KEY (audit_user) REFERENCES user_account (usr_id);
