-- primary keys

CREATE UNIQUE INDEX prosecutors_pk ON darts.prosecutors(pro_id);
ALTER  TABLE darts.prosecutors              ADD PRIMARY KEY USING INDEX prosecutors_pk;

CREATE UNIQUE INDEX defence_pk ON darts.defence(dfc_id);
ALTER  TABLE darts.defence              ADD PRIMARY KEY USING INDEX defence_pk;

CREATE UNIQUE INDEX defendants_pk ON darts.defendants(dfd_id);
ALTER  TABLE darts.defendants              ADD PRIMARY KEY USING INDEX defendants_pk;

CREATE UNIQUE INDEX judges_pk ON darts.judges(jud_id);
ALTER  TABLE darts.judges              ADD PRIMARY KEY USING INDEX judges_pk;
