delete
from security_group_courthouse_ae
where grp_id in (select grp_id from security_group where group_name in ('moj_ch_maple_house_appr', 'moj_ch_maple_house_staff'))
  and cth_id = (select cth_id from courthouse where courthouse_name = 'MAPLE HOUSE')