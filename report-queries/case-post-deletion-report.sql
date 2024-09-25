select ch.courthouse_name, c.case_number, c.data_anonymised_ts
from darts.court_case c
join darts.courthouse ch on ch.cth_id =c.cth_id
where c.is_data_anonymised=true
and c.data_anonymised_ts BETWEEN current_timestamp - interval '30 day' and  current_timestamp - interval '1 day'
order by ch.courthouse_name, c.case_number asc;