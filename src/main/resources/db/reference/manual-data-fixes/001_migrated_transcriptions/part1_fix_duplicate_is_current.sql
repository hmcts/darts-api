begin;
savepoint dmp5133;

--
-- pre-check list of TRANSCRIPTION records to be updated to set IS_CURRENT to FALSE( expect 57 )
--
select a.tra_id
from transcription a
   , transcription b
where a.chronicle_id = b.chronicle_id
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current
and b.is_current
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- we know all candidates are initial
order by a.tra_id;

--
-- pre-check list to be actioned on HEARING_TRANSCRIPTION_AE, removing links where transcription is now inactive ( expect 56 )
--
select distinct tra_id
from hearing_transcription_ae
where tra_id in (
    select a.tra_id
    from transcription a
       , transcription b
    where a.chronicle_id = b.chronicle_id
    and a.tra_id != b.tra_id                                 -- need different tra_id pairs
    and a.is_current                                         -- both tra must be current
    and b.is_current
    and a.created_ts < b.created_ts                          -- just to ensure we dont get 2 rows in the result for each pair
    and a.version_label = '1.0'                              -- we know all candidates are initial
);

--
-- pre-check list to be actioned on CASE_TRANSCRIPTION_AE, removing links where transcription is now inactive ( expect 1 )
--
select cas_id,tra_id
from case_transcription_ae
where tra_id in (
    select a.tra_id
    from transcription a
       , transcription b
    where a.chronicle_id = b.chronicle_id
    and a.tra_id != b.tra_id                                 -- need different tra_id pairs
    and a.is_current                                         -- both tra must be current
    and b.is_current
    and a.created_ts < b.created_ts                          -- just to ensure we dont get 2 rows in the result for each pair
    and a.version_label = '1.0'                              -- we know all candidates are initial
);

--
-- fixup TRANSCRIPTION data ( expect 57 )
--
update transcription
set is_current = false
where tra_id in (
    select a.tra_id
    from transcription a
       , transcription b
    where a.chronicle_id = b.chronicle_id
    and a.tra_id != b.tra_id                                 -- need different tra_id pairs
    and a.is_current                                         -- both tra must be current
    and b.is_current
    and a.created_ts < b.created_ts                          -- just to ensure we dont get 2 rows in the result for each pair
    and a.version_label = '1.0'                              -- we know all candidates are initial
);

--
-- fixup HEARING_TRANSCRIPTION_AE data ( expect 56 )
--
delete from hearing_transcription_ae
where tra_id in (
    select a.tra_id
    from transcription a
       , transcription b
    where a.chronicle_id = b.chronicle_id
    and a.tra_id != b.tra_id                                 -- need different tra_id pairs
    and a.is_current                                         -- both tra must be current
    and b.is_current
    and a.created_ts < b.created_ts                          -- just to ensure we dont get 2 rows in the result for each pair
    and a.version_label = '1.0'                              -- we know all candidates are initial
);

--
-- fixup CASE_TRANSCRIPTION_AE data ( expect 1 )
--
delete from case_transcription_ae
where tra_id in (
    select a.tra_id
    from transcription a
       , transcription b
    where a.chronicle_id = b.chronicle_id
    and a.tra_id != b.tra_id                                 -- need different tra_id pairs
    and a.is_current                                         -- both tra must be current
    and b.is_current
    and a.created_ts < b.created_ts                          -- just to ensure we dont get 2 rows in the result for each pair
    and a.version_label = '1.0'                              -- we know all candidates are initial
);

--
-- post-check verify list on TRANSCRIPTION to be actioned is now empty ( expect 0)
--
select a.tra_id
from transcription a
   , transcription b
where a.chronicle_id = b.chronicle_id
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current
and b.is_current
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- we know all candidates are initial
order by a.tra_id;

--
-- post_check verify all remaining is_current record ( one of each pair) are linked in CASE_TRANSCRIPTION_AE or HEARING_TRANSCRIPTION_AE ( expect 57 )
--
select distinct tra_id
from hearing_transcription_ae
where tra_id in (
select b.tra_id
from transcription a
   , transcription b
where a.chronicle_id = b.chronicle_id
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current
and b.is_current
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- we know all candidates are initial
) union
select distinct tra_id
from case_transcription_ae
where tra_id in (
select b.tra_id
from transcription a
   , transcription b
where a.chronicle_id = b.chronicle_id
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current
and b.is_current
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- we know all candidates are initial
);


-- script is currently configured to rollback, if run end-to-end
rollback to dmp5133;
--commit;
