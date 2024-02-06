ALTER TABLE audit
  ADD CONSTRAINT audit_case_fk
    FOREIGN KEY (cas_id) REFERENCES court_case (cas_id);

ALTER TABLE audit
  ADD CONSTRAINT audit_audit_activity_fk
    FOREIGN KEY (aua_id) REFERENCES audit_activity (aua_id);

ALTER TABLE audit
  ADD CONSTRAINT audit_user_account_fk
    FOREIGN KEY (usr_id) REFERENCES user_account (usr_id);

ALTER TABLE transcription
  ADD CONSTRAINT transcription_urgency_fk
    FOREIGN KEY (tru_id) REFERENCES transcription_urgency (tru_id);


insert into user_account(usr_id, description)
values (-1, 'Event Processor');
insert into user_account(usr_id, description)
values (-2, 'DailyList Processor');
insert into user_account(usr_id, description)
values (-3, 'AddAudio Processor');
insert into user_account(usr_id, description)
values (-4, 'AddCase Processor');
