CREATE EXTENSION IF NOT EXISTS pg_trgm;
DROP INDEX dfd_dn_idx;
CREATE INDEX dfd_dn_idx CONCURRENTLY ON defendant USING gin (defendant_name gin_trgm_ops);