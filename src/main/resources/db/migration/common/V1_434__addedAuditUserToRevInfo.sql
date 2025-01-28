alter table revinfo
    add column audit_user integer;
alter table revinfo
    add foreign key (audit_user) references user_account (usr_id);