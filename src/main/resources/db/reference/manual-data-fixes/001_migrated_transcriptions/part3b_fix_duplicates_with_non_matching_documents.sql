--3b   to act on records where we have pairs of transcriptions with differing document checksums
-- pre-check list to be actioned ( expect 7, [36618,37111,40775,46676,47589,49173,51721,57933,58481])

select a.tra_id, b.tra_id,tda.trd_id,tdb.trd_id
from transcription a
   , transcription b 
   , transcription_document tda
   , transcription_document tdb
where a.chronicle_id = b.chronicle_id 
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current                 
and b.is_current 
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- not critical, but we know all candidates are 1.0
and a.tra_id = tda.tra_id                                    -- inner join to its td table
and b.tra_id = tdb.tra_id                                    -- inner join to its td table.
and tda.checksum !=tdb.checksum                              -- and their checksums dont match
order by a.tra_id;

-- fixup on transcription to mark older as non-current
update transcription
set is_current= false
where tra_id in ( 
    select a.tra_id
    from transcription a
       , transcription b 
       , transcription_document tda
       , transcription_document tdb
    where a.chronicle_id = b.chronicle_id 
    and a.tra_id != b.tra_id                                     -- need different tra_id pairs
    and a.is_current                                             -- both tra must be current                 
    and b.is_current 
    and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
    and a.version_label = '1.0'                                  -- not critical, but we know all candidates are 1.0
    and a.tra_id = tda.tra_id                                    -- inner join to its td table
    and b.tra_id = tdb.tra_id                                    -- inner join to its td table.
    and tda.checksum !=tdb.checksum                              -- and their checksums dont match
);

-- fixup on transcription_document to swap pairs between transcriptions
-- because we are trying to swap A to B and B to A, whichever we do first will overwrite the value we need to do the second part of the swap
-- this could be solved with an intial common table expression to store the pairs, but given this is 9 swaps, seems easier to auto generate the updates as follows

-- check the current state  as follows

select a.tra_id, b.tra_id,tda.trd_id,tdb.trd_id                                 
from transcription a
   , transcription b
   , transcription_document tda
   , transcription_document tdb
where a.chronicle_id = b.chronicle_id
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current                 
and b.is_current
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- not critical, but we know all candidates are 1.0
and a.tra_id = tda.tra_id                                    -- inner join to its td table
and b.tra_id = tdb.tra_id                                    -- inner join to its td table.
and tda.checksum !=tdb.checksum                               -- and their checksums match
order by a.tra_id
;

--tra_id | tra_id | trd_id | trd_id
----------+--------+--------+--------
--  36618 |  36616 |  17329 |  17275
--  37111 |  37107 |  17318 |  17295
--  40775 |  40773 |  17319 |  17297
--  46676 |  46675 |  17322 |  17312
--  47589 |  47588 |  17323 |  17260
--  49173 |  49172 |  17336 |  17292
--  51721 |  51714 |  17324 |  17311
--  57933 |  58089 |  17328 |  17276
--  58481 |  58477 |  17333 |  17278
--(9 rows)

-- run the query to generate the 18 update statements ( 9 pairs )

select 'update transcription_document set tra_id ='||a.tra_id||' where trd_id='||tdb.trd_id||';'
from transcription a
   , transcription b
   , transcription_document tda
   , transcription_document tdb
where a.chronicle_id = b.chronicle_id
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current                 
and b.is_current
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- not critical, but we know all candidates are 1.0
and a.tra_id = tda.tra_id                                    -- inner join to its td table
and b.tra_id = tdb.tra_id                                    -- inner join to its td table.
and tda.checksum !=tdb.checksum                               -- and their checksums match
union            
select 'update transcription_document set tra_id ='||b.tra_id||' where trd_id='||tda.trd_id
from transcription a
   , transcription b
   , transcription_document tda
   , transcription_document tdb
where a.chronicle_id = b.chronicle_id
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current                 
and b.is_current
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- not critical, but we know all candidates are 1.0
and a.tra_id = tda.tra_id                                    -- inner join to its td table
and b.tra_id = tdb.tra_id                                    -- inner join to its td table.
and tda.checksum !=tdb.checksum                               -- and their checksums match
order by 1;

-- or we simply review and run the updates that result from this SQL

--update transcription_document set tra_id =36616 where trd_id=17329;
--update transcription_document set tra_id =36618 where trd_id=17275;
--update transcription_document set tra_id =37107 where trd_id=17318;
--update transcription_document set tra_id =37111 where trd_id=17295;
--update transcription_document set tra_id =40773 where trd_id=17319;
--update transcription_document set tra_id =40775 where trd_id=17297;
--update transcription_document set tra_id =46675 where trd_id=17322;
--update transcription_document set tra_id =46676 where trd_id=17312;
--update transcription_document set tra_id =47588 where trd_id=17323;
--update transcription_document set tra_id =47589 where trd_id=17260;
--update transcription_document set tra_id =49172 where trd_id=17336;
--update transcription_document set tra_id =49173 where trd_id=17292;
--update transcription_document set tra_id =51714 where trd_id=17324;
--update transcription_document set tra_id =51721 where trd_id=17311;
--update transcription_document set tra_id =57933 where trd_id=17276;
--update transcription_document set tra_id =58089 where trd_id=17328;
--update transcription_document set tra_id =58477 where trd_id=17333;
--update transcription_document set tra_id =58481 where trd_id=17278;



-- check the current state upon completion of the swaps

select a.tra_id, b.tra_id,tda.trd_id,tdb.trd_id                                 
from transcription a
   , transcription b
   , transcription_document tda
   , transcription_document tdb
where a.chronicle_id = b.chronicle_id
and a.tra_id != b.tra_id                                     -- need different tra_id pairs
and a.is_current                                             -- both tra must be current                 
and b.is_current
and a.created_ts < b.created_ts                              -- just to ensure we dont get 2 rows in the result for each pair
and a.version_label = '1.0'                                  -- not critical, but we know all candidates are 1.0
and a.tra_id = tda.tra_id                                    -- inner join to its td table
and b.tra_id = tdb.tra_id                                    -- inner join to its td table.
and tda.checksum !=tdb.checksum                               -- and their checksums match
order by a.tra_id
;



