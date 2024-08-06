CREATE EXTENSION IF NOT EXISTS pg_trgm;
DROP INDEX dfd_dn_idx;
CREATE INDEX CONCURRENTLY dfd_dn_idx ON defendant USING gin (defendant_name gin_trgm_ops);