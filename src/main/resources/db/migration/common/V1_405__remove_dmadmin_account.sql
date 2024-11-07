update courthouse set created_by = 0 where created_by = -100;
update courthouse set last_modified_by = 0 where last_modified_by = -100;

DELETE FROM user_account WHERE usr_id = -100;

update courthouse set display_name = 'Ipswich' where cth_id=61;


