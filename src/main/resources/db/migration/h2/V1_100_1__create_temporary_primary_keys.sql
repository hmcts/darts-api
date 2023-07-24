-- primary keys

ALTER TABLE darts.prosecutors ADD CONSTRAINT prosecutors_pk PRIMARY KEY(pro_id);
ALTER TABLE darts.defence ADD CONSTRAINT defence_pk PRIMARY KEY(dfc_id);
ALTER TABLE darts.defendants ADD CONSTRAINT defendants_pk PRIMARY KEY(dfd_id);
ALTER TABLE darts.judges ADD CONSTRAINT judges_pk PRIMARY KEY(jud_id);



