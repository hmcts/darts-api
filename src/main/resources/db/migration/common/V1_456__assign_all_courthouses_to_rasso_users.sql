WITH new_inserts AS (
    -- Relate all existing courthouses to the rasso_users security group. Capture only the newly added rows, ignoring
    -- any existing relations that may exist, such that only new additions will be audited.
    INSERT INTO security_group_courthouse_ae (grp_id, cth_id)
        SELECT sg.grp_id, c.cth_id
        FROM courthouse c
                 JOIN security_group sg ON sg.group_name = 'rasso_users'
        ON CONFLICT DO NOTHING
        RETURNING grp_id, cth_id
),
     new_rev AS (
         INSERT INTO revinfo (rev, revtstmp, audit_user)
             VALUES (nextval('revinfo_seq'), EXTRACT(EPOCH FROM NOW()), 0)
             RETURNING rev
     )
INSERT INTO security_group_courthouse_ae_aud (grp_id, cth_id, rev, revtype)
SELECT ni.grp_id, ni.cth_id, nr.rev, 0
FROM new_inserts ni, new_rev nr;