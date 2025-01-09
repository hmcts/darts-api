update darts.courthouse
set display_name = 'Taunton Sitting at Worle'
where courthouse_object_id = '0b170758981f894d';
update darts.courthouse
set display_name = 'Linc Crown Sitting at Linc Mags'
where courthouse_object_id = '0b170758990cab24';
update darts.courthouse
set display_name = 'Wolverhampton Sitting at Telford'
where courthouse_object_id = '0b17075899a8e97e';
update darts.courthouse
set display_name = 'Worcester Sitting at Redditch JC'
where courthouse_object_id = '0b17075899b3c802';
update darts.courthouse
set display_name = 'Kingston upon Hull Combined Court Centre (including Beverley)'
where courthouse_object_id = '0b17075880a73969';
update darts.courthouse
set display_name = 'Rolls Building Courthouse 1'
where courthouse_object_id = '0b17075880a7396d';
update darts.courthouse
set display_name = 'Rolls Building Courthouse 2'
where courthouse_object_id = '0b17075880a7396e';
update darts.courthouse
set display_name = 'Rolls Building Courthouse 3'
where courthouse_object_id = '0b17075880ad0e61';
update darts.courthouse
set display_name = 'Rolls Building Courthouse 4'
where courthouse_object_id = '0b17075880ad0e62';
update darts.courthouse
set display_name = 'West Midlands Court Centre'
where courthouse_object_id = '0b17075880ad0e65';
update darts.courthouse
set display_name = 'Crown Court Sitting At St Albans Magistrates Court'
where courthouse_object_id = '0b17075880b40a0c';
update darts.courthouse
set display_name = 'Newcastle Moot Hall'
where courthouse_object_id = '0b17075880cd641a';


insert into darts.courthouse(courthouse_name, display_name, folder_path, courthouse_object_id)
values ('BURNLEY MAGS TWO', 'Burnley Mags Two', '/Area 2/Burnley Mags Two', '0b1707589a44e76c');

update darts.event_handler
set event_name = 'Disqualification Order (from working with children) - ADULTS'
where event_type = '40750'
  and event_sub_type = '12400';
update darts.event_handler
set event_name = 'Disqualification Order (from working with children) - JUVENILES'
where event_type = '40750'
  and event_sub_type = '12401';
update darts.event_handler
set event_name = 'Disqualification Order (from working with children) - ADULTS'
where event_type = '40751'
  and event_sub_type = '12400';
update darts.event_handler
set event_name = 'Disqualification Order (from working with children) - JUVENILES'
where event_type = '40751'
  and event_sub_type = '12401';
update darts.event_handler
set event_name = 'Disqualification Order (from working with children) - ADULTS'
where event_type = '40752'
  and event_sub_type = '12400';
update darts.event_handler
set event_name = 'Disqualification Order (from working with children) - JUVENILES'
where event_type = '40752'
  and event_sub_type = '12401';
update darts.event_handler
set event_name = 'Disqualification Order (from working with children) - ADULTS'
where event_type = '40753'
  and event_sub_type = '12400';
update darts.event_handler
set event_name = 'Disqualification Order (from working with children) - JUVENILES'
where event_type = '40753'
  and event_sub_type = '12401';


update darts.security_group
set rol_id = (select rol_id from darts.security_role where role_name = 'DARTS')
where group_name = 'MEDIA_IN_PERPETUITY';