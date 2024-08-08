CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX dfd_dn_trgm_idx ON defendant USING gin (defendant_name gin_trgm_ops);