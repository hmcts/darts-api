INSERT INTO darts.retention_policy_type (rpt_id,policy_name,display_name,fixed_policy_key,duration,policy_start_ts,policy_end_ts,retention_policy_object_id,created_ts,created_by,last_modified_ts,last_modified_by,description) VALUES
(nextval('rpt_seq'),'DARTS Archive Permanent Retention','Archive Permanent',-3,'30Y0M0D','2024-01-01T00:00:00Z',null,null,current_timestamp,0,current_timestamp,0,'DARTS Archive Permanent Retention'),
(nextval('rpt_seq'),'DARTS Archive Standard Retention','Archive Standard',-4,'7Y0M0D','2024-01-01T00:00:00Z',null,null,current_timestamp,0,current_timestamp,0,'DARTS Archive Standard Retention');
