select ch.courthouse_name, c.case_number, cr.retain_until_ts
from darts.court_case c
join darts.case_retention cr on cr.cas_id=c.cas_id
join darts.courthouse ch on ch.cth_id =c.cth_id
and cr.current_state='COMPLETE'
where c.is_data_anonymised=false
and cr.retain_until_ts< current_timestamp + interval '30 day'
order by ch.courthouse_name, c.case_number asc