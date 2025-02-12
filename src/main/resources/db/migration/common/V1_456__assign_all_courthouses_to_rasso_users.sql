INSERT INTO security_group_courthouse_ae (grp_id, cth_id)
SELECT sg.grp_id, c.cth_id
FROM courthouse c
         JOIN security_group sg ON sg.group_name = 'rasso_users'
ON CONFLICT DO NOTHING;
