UPDATE security_role
set role_name='REQUESTER'
WHERE rol_id = 2;

UPDATE security_role
set role_name='APPROVER'
WHERE rol_id = 1;

UPDATE security_role
set role_name='JUDGE'
WHERE rol_id = 3;

UPDATE security_role
set role_name='TRANSCRIBER'
WHERE rol_id = 4;

UPDATE security_role
set role_name='LANGUAGE_SHOP_USER'
WHERE rol_id = 5;

INSERT into security_role (rol_id, role_name) values (6, 'RCJ_APPEALS');
ALTER SEQUENCE rol_seq RESTART WITH 7;
