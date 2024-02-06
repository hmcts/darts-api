SET search_path TO darts;

INSERT INTO courthouse (cth_id, courthouse_name, courthouse_code, created_ts, last_modified_ts)
VALUES (1, 'SWANSEA', 457, '2023-05-06 00:00:00+00', '2023-06-14 16:45:09.998033+00');
INSERT INTO courthouse (cth_id, courthouse_name, courthouse_code, created_ts, last_modified_ts)
VALUES (2, 'CARDIFF', 458, '2023-05-06 00:00:00+00', '2023-06-14 16:45:09.998033+00');
INSERT INTO courthouse (cth_id, courthouse_name, courthouse_code, created_ts, last_modified_ts)
VALUES (3, 'BIRMINGHAM', 459, '2023-05-06 00:00:00+00', '2023-06-14 16:45:09.998033+00');
INSERT INTO courthouse (cth_id, courthouse_name, courthouse_code, created_ts, last_modified_ts)
VALUES (4, 'LIVERPOOL', 460, '2023-05-06 00:00:00+00', '2023-06-14 16:45:09.998033+00');
INSERT INTO courthouse (cth_id, courthouse_name, courthouse_code, created_ts, last_modified_ts)
VALUES (5, 'MANCHESTER', 461, '2023-05-06 00:00:00+00', '2023-06-14 16:45:09.998033+00');
ALTER SEQUENCE cth_seq RESTART WITH 20;

INSERT INTO audit_activity
VALUES (998, 'test name', 'test description');
INSERT INTO darts.audit_activity(aua_id, activity_name, activity_description, created_ts, created_by, last_modified_ts, last_modified_by)
VALUES (998, 'test name', 'test description', '2023-05-06T00:00:00+00', 0, '2023-05-06T00:00:00+00', 0);
INSERT INTO audit
VALUES (999, 2, 998, 4, '2023-06-13T08:13:09.688537759Z', 'application_server', 'additional_data');

INSERT INTO courtroom
VALUES (1, 1, '1');
INSERT INTO courtroom
VALUES (2, 1, '2');
INSERT INTO courtroom
VALUES (3, 1, '3');
INSERT INTO courtroom
VALUES (4, 1, '4');
INSERT INTO courtroom
VALUES (5, 2, '1');
INSERT INTO courtroom
VALUES (6, 2, '2');
INSERT INTO courtroom
VALUES (7, 2, '3');
INSERT INTO courtroom
VALUES (8, 2, '4');
INSERT INTO courtroom
VALUES (9, 3, '1');
INSERT INTO courtroom
VALUES (10, 3, '2');
INSERT INTO courtroom
VALUES (11, 3, '3');
INSERT INTO courtroom
VALUES (12, 3, '4');
INSERT INTO courtroom
VALUES (13, 4, '1');
INSERT INTO courtroom
VALUES (14, 4, '2');
INSERT INTO courtroom
VALUES (15, 4, '3');
INSERT INTO courtroom
VALUES (16, 4, '4');
ALTER SEQUENCE ctr_seq RESTART WITH 20;


INSERT INTO court_case
VALUES (1, null, null, 'Case0000001', null, null, null, ARRAY ['Mr Defendant0000001 Bloggs1','Mr Defendant0000001 Bloggs2'],
        ARRAY ['Prosecutor00000011','Prosecutor00000012'], ARRAY ['Defence00000011','Defence00000012'], null, null, null);
INSERT INTO court_case
VALUES (2, null, null, 'Case0000002', null, null, null, ARRAY ['Mr Defendant0000002 Bloggs1','Mr Defendant0000002 Bloggs2'],
        ARRAY ['Prosecutor00000021','Prosecutor00000022'], ARRAY ['Defence00000021','Defence00000022'], null, null, null);
INSERT INTO court_case
VALUES (3, null, null, 'Case0000003', null, null, null, ARRAY ['Mr Defendant0000003 Bloggs1','Mr Defendant0000003 Bloggs2'],
        ARRAY ['Prosecutor00000031','Prosecutor00000032'], ARRAY ['Defence00000031','Defence00000032'], null, null, null);
INSERT INTO court_case
VALUES (4, null, null, 'Case0000004', null, null, null, ARRAY ['Mr Defendant0000004 Bloggs1','Mr Defendant0000004 Bloggs2'],
        ARRAY ['Prosecutor00000041','Prosecutor00000042'], ARRAY ['Defence00000041','Defence00000042'], null, null, null);
INSERT INTO court_case
VALUES (5, null, null, 'Case0000005', null, null, null, ARRAY ['Mr Defendant0000005 Bloggs1','Mr Defendant0000005 Bloggs2'],
        ARRAY ['Prosecutor00000051','Prosecutor00000052'], ARRAY ['Defence00000051','Defence00000052'], null, null, null);
INSERT INTO court_case
VALUES (6, null, null, 'Case0000006', null, null, null, ARRAY ['Mr Defendant0000006 Bloggs1','Mr Defendant0000006 Bloggs2'],
        ARRAY ['Prosecutor00000061','Prosecutor00000062'], ARRAY ['Defence00000061','Defence00000062'], null, null, null);
INSERT INTO court_case
VALUES (7, null, null, 'Case0000007', null, null, null, ARRAY ['Mr Defendant0000007 Bloggs1','Mr Defendant0000007 Bloggs2'],
        ARRAY ['Prosecutor00000071','Prosecutor00000072'], ARRAY ['Defence00000071','Defence00000072'], null, null, null);
INSERT INTO court_case
VALUES (8, null, null, 'Case0000008', null, null, null, ARRAY ['Mr Defendant0000008 Bloggs1','Mr Defendant0000008 Bloggs2'],
        ARRAY ['Prosecutor00000081','Prosecutor00000082'], ARRAY ['Defence00000081','Defence00000082'], null, null, null);
INSERT INTO court_case
VALUES (9, null, null, 'Case0000009', null, null, null, ARRAY ['Mr Defendant0000009 Bloggs1','Mr Defendant0000009 Bloggs2'],
        ARRAY ['Prosecutor00000091','Prosecutor00000092'], ARRAY ['Defence00000091','Defence00000092'], null, null, null);
INSERT INTO court_case
VALUES (10, null, null, 'Case0000010', null, null, null, ARRAY ['Mr Defendant0000010 Bloggs1','Mr Defendant0000010 Bloggs2'],
        ARRAY ['Prosecutor00000101','Prosecutor00000102'], ARRAY ['Defence00000101','Defence00000102'], null, null, null);
INSERT INTO court_case
VALUES (11, null, null, 'Case0000011', null, null, null, ARRAY ['Mr Defendant0000011 Bloggs1','Mr Defendant0000011 Bloggs2'],
        ARRAY ['Prosecutor00000111','Prosecutor00000112'], ARRAY ['Defence00000111','Defence00000112'], null, null, null);
INSERT INTO court_case
VALUES (12, null, null, 'Case0000012', null, null, null, ARRAY ['Mr Defendant0000012 Bloggs1','Mr Defendant0000012 Bloggs2'],
        ARRAY ['Prosecutor00000121','Prosecutor00000122'], ARRAY ['Defence00000121','Defence00000122'], null, null, null);
INSERT INTO court_case
VALUES (13, null, null, 'Case0000013', null, null, null, ARRAY ['Mr Defendant0000013 Bloggs1','Mr Defendant0000013 Bloggs2'],
        ARRAY ['Prosecutor00000131','Prosecutor00000132'], ARRAY ['Defence00000131','Defence00000132'], null, null, null);
INSERT INTO court_case
VALUES (14, null, null, 'Case0000014', null, null, null, ARRAY ['Mr Defendant0000014 Bloggs1','Mr Defendant0000014 Bloggs2'],
        ARRAY ['Prosecutor00000141','Prosecutor00000142'], ARRAY ['Defence00000141','Defence00000142'], null, null, null);
INSERT INTO court_case
VALUES (15, null, null, 'Case0000015', null, null, null, ARRAY ['Mr Defendant0000015 Bloggs1','Mr Defendant0000015 Bloggs2'],
        ARRAY ['Prosecutor00000151','Prosecutor00000152'], ARRAY ['Defence00000151','Defence00000152'], null, null, null);
INSERT INTO court_case
VALUES (16, null, null, 'Case0000016', null, null, null, ARRAY ['Mr Defendant0000016 Bloggs1','Mr Defendant0000016 Bloggs2'],
        ARRAY ['Prosecutor00000161','Prosecutor00000162'], ARRAY ['Defence00000161','Defence00000162'], null, null, null);
INSERT INTO court_case
VALUES (17, null, null, 'Case0000017', null, null, null, ARRAY ['Mr Defendant0000017 Bloggs1','Mr Defendant0000017 Bloggs2'],
        ARRAY ['Prosecutor00000171','Prosecutor00000172'], ARRAY ['Defence00000171','Defence00000172'], null, null, null);
INSERT INTO court_case
VALUES (18, null, null, 'Case0000018', null, null, null, ARRAY ['Mr Defendant0000018 Bloggs1','Mr Defendant0000018 Bloggs2'],
        ARRAY ['Prosecutor00000181','Prosecutor00000182'], ARRAY ['Defence00000181','Defence00000182'], null, null, null);
INSERT INTO court_case
VALUES (19, null, null, 'Case0000019', null, null, null, ARRAY ['Mr Defendant0000019 Bloggs1','Mr Defendant0000019 Bloggs2'],
        ARRAY ['Prosecutor00000191','Prosecutor00000192'], ARRAY ['Defence00000191','Defence00000192'], null, null, null);
INSERT INTO court_case
VALUES (20, null, null, 'Case0000020', null, null, null, ARRAY ['Mr Defendant0000020 Bloggs1','Mr Defendant0000020 Bloggs2'],
        ARRAY ['Prosecutor00000201','Prosecutor00000202'], ARRAY ['Defence00000201','Defence00000202'], null, null, null);
INSERT INTO court_case
VALUES (21, null, null, 'Case0000021', null, null, null, ARRAY ['Mr Defendant0000021 Bloggs1','Mr Defendant0000021 Bloggs2'],
        ARRAY ['Prosecutor00000211','Prosecutor00000212'], ARRAY ['Defence00000211','Defence00000212'], null, null, null);
INSERT INTO court_case
VALUES (22, null, null, 'Case0000022', null, null, null, ARRAY ['Mr Defendant0000022 Bloggs1','Mr Defendant0000022 Bloggs2'],
        ARRAY ['Prosecutor00000221','Prosecutor00000222'], ARRAY ['Defence00000221','Defence00000222'], null, null, null);
INSERT INTO court_case
VALUES (23, null, null, 'Case0000023', null, null, null, ARRAY ['Mr Defendant0000023 Bloggs1','Mr Defendant0000023 Bloggs2'],
        ARRAY ['Prosecutor00000231','Prosecutor00000232'], ARRAY ['Defence00000231','Defence00000232'], null, null, null);
INSERT INTO court_case
VALUES (24, null, null, 'Case0000024', null, null, null, ARRAY ['Mr Defendant0000024 Bloggs1','Mr Defendant0000024 Bloggs2'],
        ARRAY ['Prosecutor00000241','Prosecutor00000242'], ARRAY ['Defence00000241','Defence00000242'], null, null, null);
INSERT INTO court_case
VALUES (25, null, null, 'Case0000025', null, null, null, ARRAY ['Mr Defendant0000025 Bloggs1','Mr Defendant0000025 Bloggs2'],
        ARRAY ['Prosecutor00000251','Prosecutor00000252'], ARRAY ['Defence00000251','Defence00000252'], null, null, null);
INSERT INTO court_case
VALUES (26, null, null, 'Case0000026', null, null, null, ARRAY ['Mr Defendant0000026 Bloggs1','Mr Defendant0000026 Bloggs2'],
        ARRAY ['Prosecutor00000261','Prosecutor00000262'], ARRAY ['Defence00000261','Defence00000262'], null, null, null);
INSERT INTO court_case
VALUES (27, null, null, 'Case0000027', null, null, null, ARRAY ['Mr Defendant0000027 Bloggs1','Mr Defendant0000027 Bloggs2'],
        ARRAY ['Prosecutor00000271','Prosecutor00000272'], ARRAY ['Defence00000271','Defence00000272'], null, null, null);
INSERT INTO court_case
VALUES (28, null, null, 'Case0000028', null, null, null, ARRAY ['Mr Defendant0000028 Bloggs1','Mr Defendant0000028 Bloggs2'],
        ARRAY ['Prosecutor00000281','Prosecutor00000282'], ARRAY ['Defence00000281','Defence00000282'], null, null, null);
INSERT INTO court_case
VALUES (29, null, null, 'Case0000029', null, null, null, ARRAY ['Mr Defendant0000029 Bloggs1','Mr Defendant0000029 Bloggs2'],
        ARRAY ['Prosecutor00000291','Prosecutor00000292'], ARRAY ['Defence00000291','Defence00000292'], null, null, null);
INSERT INTO court_case
VALUES (30, null, null, 'Case0000030', null, null, null, ARRAY ['Mr Defendant0000030 Bloggs1','Mr Defendant0000030 Bloggs2'],
        ARRAY ['Prosecutor00000301','Prosecutor00000302'], ARRAY ['Defence00000301','Defence00000302'], null, null, null);
INSERT INTO court_case
VALUES (31, null, null, 'Case0000031', null, null, null, ARRAY ['Mr Defendant0000031 Bloggs1','Mr Defendant0000031 Bloggs2'],
        ARRAY ['Prosecutor00000311','Prosecutor00000312'], ARRAY ['Defence00000311','Defence00000312'], null, null, null);
INSERT INTO court_case
VALUES (32, null, null, 'Case0000032', null, null, null, ARRAY ['Mr Defendant0000032 Bloggs1','Mr Defendant0000032 Bloggs2'],
        ARRAY ['Prosecutor00000321','Prosecutor00000322'], ARRAY ['Defence00000321','Defence00000322'], null, null, null);
INSERT INTO court_case
VALUES (33, null, null, 'Case0000033', null, null, null, ARRAY ['Mr Defendant0000033 Bloggs1','Mr Defendant0000033 Bloggs2'],
        ARRAY ['Prosecutor00000331','Prosecutor00000332'], ARRAY ['Defence00000331','Defence00000332'], null, null, 1);
ALTER SEQUENCE cas_seq RESTART WITH 50;

INSERT INTO hearing
VALUES (1, 1, 1, '{Judge1}', '2023-06-20', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (2, 2, 1, '{Judge1}', '2023-06-20', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (3, 3, 1, '{Judge1}', '2023-06-20', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (4, 4, 1, '{Judge1}', '2023-06-20', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (5, 5, 1, '{Judge1}', '2023-06-20', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (6, 6, 1, '{Judge1}', '2023-06-20', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (7, 7, 1, '{Judge1}', '2023-06-20', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (8, 8, 1, '{Judge1}', '2023-06-20', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (9, 9, 2, '{Judge2}', '2023-06-20', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (10, 10, 2, '{Judge2}', '2023-06-20', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (11, 11, 2, '{Judge2}', '2023-06-20', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (12, 12, 2, '{Judge2}', '2023-06-20', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (13, 13, 2, '{Judge2}', '2023-06-20', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (14, 14, 2, '{Judge2}', '2023-06-20', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (15, 15, 2, '{Judge2}', '2023-06-20', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (16, 16, 2, '{Judge2}', '2023-06-20', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (17, 17, 3, '{Judge3}', '2023-06-20', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (18, 18, 3, '{Judge3}', '2023-06-20', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (19, 19, 3, '{Judge3}', '2023-06-20', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (20, 20, 3, '{Judge3}', '2023-06-20', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (21, 21, 3, '{Judge3}', '2023-06-20', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (22, 22, 3, '{Judge3}', '2023-06-20', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (23, 23, 3, '{Judge3}', '2023-06-20', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (24, 24, 3, '{Judge3}', '2023-06-20', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (25, 25, 4, '{Judge4}', '2023-06-20', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (26, 26, 4, '{Judge4}', '2023-06-20', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (27, 27, 4, '{Judge4}', '2023-06-20', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (28, 28, 4, '{Judge4}', '2023-06-20', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (29, 29, 4, '{Judge4}', '2023-06-20', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (30, 30, 4, '{Judge4}', '2023-06-20', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (31, 31, 4, '{Judge4}', '2023-06-20', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (32, 32, 4, '{Judge4}', '2023-06-20', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (33, 1, 1, '{Judge1}', '2023-06-21', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (34, 2, 1, '{Judge1}', '2023-06-21', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (35, 3, 1, '{Judge1}', '2023-06-21', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (36, 4, 1, '{Judge1}', '2023-06-21', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (37, 5, 1, '{Judge1}', '2023-06-21', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (38, 6, 1, '{Judge1}', '2023-06-21', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (39, 7, 1, '{Judge1}', '2023-06-21', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (40, 8, 1, '{Judge1}', '2023-06-21', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (41, 9, 2, '{Judge2}', '2023-06-21', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (42, 10, 2, '{Judge2}', '2023-06-21', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (43, 11, 2, '{Judge2}', '2023-06-21', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (44, 12, 2, '{Judge2}', '2023-06-21', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (45, 13, 2, '{Judge2}', '2023-06-21', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (46, 14, 2, '{Judge2}', '2023-06-21', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (47, 15, 2, '{Judge2}', '2023-06-21', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (48, 16, 2, '{Judge2}', '2023-06-21', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (49, 17, 3, '{Judge3}', '2023-06-21', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (50, 18, 3, '{Judge3}', '2023-06-21', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (51, 19, 3, '{Judge3}', '2023-06-21', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (52, 20, 3, '{Judge3}', '2023-06-21', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (53, 21, 3, '{Judge3}', '2023-06-21', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (54, 22, 3, '{Judge3}', '2023-06-21', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (55, 23, 3, '{Judge3}', '2023-06-21', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (56, 24, 3, '{Judge3}', '2023-06-21', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (57, 25, 4, '{Judge4}', '2023-06-21', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (58, 26, 4, '{Judge4}', '2023-06-21', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (59, 27, 4, '{Judge4}', '2023-06-21', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (60, 28, 4, '{Judge4}', '2023-06-21', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (61, 29, 4, '{Judge4}', '2023-06-21', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (62, 30, 4, '{Judge4}', '2023-06-21', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (63, 31, 4, '{Judge4}', '2023-06-21', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (64, 32, 4, '{Judge4}', '2023-06-21', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (65, 1, 1, '{Judge1}', '2023-06-22', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (66, 2, 1, '{Judge1}', '2023-06-22', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (67, 3, 1, '{Judge1}', '2023-06-22', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (68, 4, 1, '{Judge1}', '2023-06-22', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (69, 5, 1, '{Judge1}', '2023-06-22', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (70, 6, 1, '{Judge1}', '2023-06-22', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (71, 7, 1, '{Judge1}', '2023-06-22', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (72, 8, 1, '{Judge1}', '2023-06-22', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (73, 9, 2, '{Judge2}', '2023-06-22', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (74, 10, 2, '{Judge2}', '2023-06-22', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (75, 11, 2, '{Judge2}', '2023-06-22', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (76, 12, 2, '{Judge2}', '2023-06-22', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (77, 13, 2, '{Judge2}', '2023-06-22', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (78, 14, 2, '{Judge2}', '2023-06-22', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (79, 15, 2, '{Judge2}', '2023-06-22', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (80, 16, 2, '{Judge2}', '2023-06-22', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (81, 17, 3, '{Judge3}', '2023-06-22', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (82, 18, 3, '{Judge3}', '2023-06-22', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (83, 19, 3, '{Judge3}', '2023-06-22', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (84, 20, 3, '{Judge3}', '2023-06-22', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (85, 21, 3, '{Judge3}', '2023-06-22', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (86, 22, 3, '{Judge3}', '2023-06-22', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (87, 23, 3, '{Judge3}', '2023-06-22', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (88, 24, 3, '{Judge3}', '2023-06-22', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (89, 25, 4, '{Judge4}', '2023-06-22', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (90, 26, 4, '{Judge4}', '2023-06-22', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (91, 27, 4, '{Judge4}', '2023-06-22', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (92, 28, 4, '{Judge4}', '2023-06-22', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (93, 29, 4, '{Judge4}', '2023-06-22', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (94, 30, 4, '{Judge4}', '2023-06-22', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (95, 31, 4, '{Judge4}', '2023-06-22', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (96, 32, 4, '{Judge4}', '2023-06-22', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (97, 1, 1, '{Judge1}', '2023-06-23', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (98, 2, 1, '{Judge1}', '2023-06-23', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (99, 3, 1, '{Judge1}', '2023-06-23', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (100, 4, 1, '{Judge1}', '2023-06-23', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (101, 5, 1, '{Judge1}', '2023-06-23', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (102, 6, 1, '{Judge1}', '2023-06-23', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (103, 7, 1, '{Judge1}', '2023-06-23', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (104, 8, 1, '{Judge1}', '2023-06-23', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (105, 9, 2, '{Judge2}', '2023-06-23', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (106, 10, 2, '{Judge2}', '2023-06-23', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (107, 11, 2, '{Judge2}', '2023-06-23', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (108, 12, 2, '{Judge2}', '2023-06-23', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (109, 13, 2, '{Judge2}', '2023-06-23', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (110, 14, 2, '{Judge2}', '2023-06-23', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (111, 15, 2, '{Judge2}', '2023-06-23', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (112, 16, 2, '{Judge2}', '2023-06-23', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (113, 17, 3, '{Judge3}', '2023-06-23', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (114, 18, 3, '{Judge3}', '2023-06-23', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (115, 19, 3, '{Judge3}', '2023-06-23', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (116, 20, 3, '{Judge3}', '2023-06-23', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (117, 21, 3, '{Judge3}', '2023-06-23', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (118, 22, 3, '{Judge3}', '2023-06-23', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (119, 23, 3, '{Judge3}', '2023-06-23', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (120, 24, 3, '{Judge3}', '2023-06-23', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (121, 25, 4, '{Judge4}', '2023-06-23', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (122, 26, 4, '{Judge4}', '2023-06-23', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (123, 27, 4, '{Judge4}', '2023-06-23', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (124, 28, 4, '{Judge4}', '2023-06-23', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (125, 29, 4, '{Judge4}', '2023-06-23', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (126, 30, 4, '{Judge4}', '2023-06-23', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (127, 31, 4, '{Judge4}', '2023-06-23', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (128, 32, 4, '{Judge4}', '2023-06-23', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (129, 1, 1, '{Judge1}', '2023-06-24', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (130, 2, 1, '{Judge1}', '2023-06-24', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (131, 3, 1, '{Judge1}', '2023-06-24', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (132, 4, 1, '{Judge1}', '2023-06-24', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (133, 5, 1, '{Judge1}', '2023-06-24', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (134, 6, 1, '{Judge1}', '2023-06-24', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (135, 7, 1, '{Judge1}', '2023-06-24', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (136, 8, 1, '{Judge1}', '2023-06-24', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (137, 9, 2, '{Judge2}', '2023-06-24', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (138, 10, 2, '{Judge2}', '2023-06-24', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (139, 11, 2, '{Judge2}', '2023-06-24', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (140, 12, 2, '{Judge2}', '2023-06-24', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (141, 13, 2, '{Judge2}', '2023-06-24', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (142, 14, 2, '{Judge2}', '2023-06-24', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (143, 15, 2, '{Judge2}', '2023-06-24', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (144, 16, 2, '{Judge2}', '2023-06-24', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (145, 17, 3, '{Judge3}', '2023-06-24', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (146, 18, 3, '{Judge3}', '2023-06-24', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (147, 19, 3, '{Judge3}', '2023-06-24', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (148, 20, 3, '{Judge3}', '2023-06-24', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (149, 21, 3, '{Judge3}', '2023-06-24', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (150, 22, 3, '{Judge3}', '2023-06-24', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (151, 23, 3, '{Judge3}', '2023-06-24', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (152, 24, 3, '{Judge3}', '2023-06-24', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (153, 25, 4, '{Judge4}', '2023-06-24', '09:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (154, 26, 4, '{Judge4}', '2023-06-24', '10:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (155, 27, 4, '{Judge4}', '2023-06-24', '11:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (156, 28, 4, '{Judge4}', '2023-06-24', '12:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (157, 29, 4, '{Judge4}', '2023-06-24', '13:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (158, 30, 4, '{Judge4}', '2023-06-24', '14:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (159, 31, 4, '{Judge4}', '2023-06-24', '15:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (160, 32, 4, '{Judge4}', '2023-06-24', '16:00:00', TRUE, NULL);
INSERT INTO hearing
VALUES (161, 33, 4, '{Judge4}', '2023-06-24', '16:00:00', TRUE, NULL);
ALTER SEQUENCE hea_seq RESTART WITH 200;

INSERT INTO darts.event (eve_id, ctr_id, evh_id, event_object_id, event_id, event_name, event_text, event_ts, case_number, version_label, message_id,
                         superseded, version)
VALUES (1, 1, 1, 'LOG', 1, 'LOG', 'TEST', '2023-07-01 10:00:00+00', '{1,2}', 'test', 'test', true, 1);

INSERT INTO darts.hearing_event_ae (hev_id, hea_id, eve_id)
VALUES (1, 1, 1);
