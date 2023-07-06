alter table moj_case add moj_cth_id integer;

alter table moj_case
    add constraint moj_case_courthouse_fk
        foreign key (moj_cth_id) references moj_courthouse (moj_cth_id);

