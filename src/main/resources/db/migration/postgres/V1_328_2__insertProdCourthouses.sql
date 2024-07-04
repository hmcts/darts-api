-- Create flyway user_accounts for Migration User and dmadmin users
-- Courthouses in Heritage DARTS are created/modified by user dmadmin,
-- so creating this user is a pre-requisite to creating the Courthouse data in flyway scripts.
--
-- Then Create the courthouse data and associate these courthouses with their agreed mapping.


-- Insert user_accounts flyway data

-- Migration default user to be added to flyway scripts.
INSERT INTO user_account (usr_id,dm_user_s_object_id,user_name,user_full_name,user_email_address,description,is_active,last_login_ts,is_system_user,account_guid,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (-99,NULL,'Migration User',NULL,'dartsmigrationuser@example.com','Migration User',false,NULL,false,'Not available',CURRENT_TIMESTAMP,-99,CURRENT_TIMESTAMP,-99);

-- dmadmin user to be added to flyway scripts.
INSERT INTO user_account (usr_id,dm_user_s_object_id,user_name,user_full_name,user_email_address,description,is_active,last_login_ts,is_system_user,account_guid,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (-100,'1117075880000102','dmadmin',NULL,'dmadmin@localhost',' ',false,'2009-06-22 20:00:54.000',false,NULL,'2016-02-10 07:43:08.000',-99,'2016-02-10 07:43:08.000',-99);

-- Insert the Courthouses referencing dmadmin user

INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (1,NULL,'4 NEWTON STREET','Birmingham Crown Court sitting at 4 Newton Street','2010-01-21 13:44:33.000',-100,'2020-05-30 15:11:39.000',-100),
	 (2,NULL,'AMERSHAM LAW COURTS','Amersham Law Courts','2021-03-23 18:12:39.000',-100,'2021-03-23 18:12:55.000',-100),
	 (3,NULL,'AYLESBURY','Aylesbury','2012-02-21 13:37:08.000',-100,'2012-02-21 13:37:27.000',-100),
	 (4,NULL,'BARNSTAPLE','Barnstaple','2011-12-01 14:42:29.000',-100,'2011-12-01 14:42:54.000',-100),
	 (5,NULL,'BARONS COURT WALSALL','Barons Court Walsall','2022-01-24 18:11:28.000',-100,'2022-01-24 18:12:37.000',-100),
	 (6,NULL,'BARROW-IN-FURNESS','Barrow-in-Furness','2011-12-07 09:55:24.000',-100,'2011-12-07 09:55:48.000',-100),
	 (7,NULL,'BASILDON','Basildon Combined Court Centre','2011-10-20 17:42:21.000',-100,'2011-10-20 17:42:54.000',-100),
	 (8,NULL,'BIRMINGHAM','Birmingham','2010-01-21 13:42:59.000',-100,'2020-05-30 15:22:29.000',-100),
	 (9,NULL,'BIRMINGHAM LIBRARY AND REP','Birmingham Library and Rep','2020-12-01 14:30:54.000',-100,'2020-12-01 14:31:38.000',-100),
	 (10,NULL,'BLACKFRIARS','Blackfriars','2011-12-01 14:43:42.000',-100,'2011-12-01 14:44:01.000',-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (11,NULL,'BOLTON','Bolton Combined Court Centre','2011-11-22 18:09:52.000',-100,'2011-11-22 18:11:23.000',-100),
	 (12,NULL,'BOLTON STADIUM','Bolton Stadium','2021-03-23 18:08:01.000',-100,'2021-03-23 18:08:16.000',-100),
	 (13,NULL,'BOURNEMOUTH CROWN AND COUNTY COURTS','Bournemouth Combined Court Centre','2009-11-06 11:51:56.000',-100,'2020-05-30 15:29:08.000',-100),
	 (14,NULL,'BRADFORD','Bradford Combined Court Centre','2012-01-20 08:37:29.000',-100,'2012-01-20 08:37:52.000',-100),
	 (15,NULL,'BRIGHTON MAGISTRATES COURT','Crown Court sitting at Brighton Magistrates Court','2009-12-10 14:40:17.000',-100,'2009-12-10 14:40:49.000',-100),
	 (16,NULL,'BRISTOL','Bristol','2011-11-10 14:36:30.000',-100,'2011-11-10 14:37:43.000',-100),
	 (17,NULL,'BURNLEY','Burnley Combined Court Centre','2011-09-21 02:50:26.000',-100,'2011-09-21 02:51:01.000',-100),
	 (18,NULL,'CAERNARFON','Caernarfon','2012-01-26 13:34:37.000',-100,'2012-01-26 13:34:57.000',-100),
	 (19,NULL,'CAMBRIDGE','Cambridge','2011-12-14 12:27:52.000',-100,'2011-12-14 12:28:18.000',-100),
	 (20,NULL,'CANTERBURY','Canterbury Combined Court Centre','2012-02-23 14:30:52.000',-100,'2012-02-23 14:31:14.000',-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (21,NULL,'CARDIFF','Cardiff','2009-10-09 11:51:14.000',-100,'2020-05-30 15:06:37.000',-100),
	 (22,NULL,'CARLISLE','Carlisle Combined Court Centre','2009-10-22 18:54:50.000',-100,'2009-10-22 18:55:20.000',-100),
	 (23,NULL,'CARMARTHEN','Carmarthen','2012-02-21 13:34:23.000',-100,'2012-02-21 13:34:57.000',-100),
	 (24,NULL,'CENTRAL CRIMINAL COURT','Central Criminal Court','2012-03-23 10:22:21.000',-100,'2012-03-23 10:22:58.000',-100),
	 (25,NULL,'CHELMSFORD','Chelmsford','2012-02-29 11:25:16.000',-100,'2012-02-29 11:26:30.000',-100),
	 (26,NULL,'CHELMSFORD NEW MAGISTRATES','Chelmsford New Magistrates','2012-03-30 12:04:12.000',-100,'2012-03-30 12:06:07.000',-100),
	 (27,NULL,'CHESTER','Chester','2012-01-20 08:25:22.000',-100,'2012-01-20 08:25:43.000',-100),
	 (28,NULL,'CHESTER CROWNE PLAZA','Chester Crowne Plaza','2021-06-21 17:40:58.000',-100,'2021-06-21 17:44:32.000',-100),
	 (29,NULL,'CHESTER MAGISTRATES COURT','Chester Magistrates Court','2023-09-08 17:24:56.000',-100,'2023-09-08 17:25:19.000',-100),
	 (30,NULL,'CHESTER TOWN HALL','Chester Town Hall','2020-09-18 18:15:29.000',-100,'2020-09-18 18:15:57.000',-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (31,NULL,'CHICHESTER','Chichester Combined Court Centre','2011-11-01 14:03:28.000',-100,'2011-11-01 14:03:37.000',-100),
	 (32,NULL,'CIRENCESTER COURTHOUSE','Cirencester','2021-01-19 17:26:51.000',-100,'2021-01-19 17:27:23.000',-100),
	 (33,NULL,'COVENTRY','Coventry Combined Court Centre','2012-03-21 11:56:02.000',-100,'2012-03-21 11:56:22.000',-100),
	 (34,NULL,'COVENTRY CROWN COURT','Coventry Crown Court','2014-03-20 13:56:28.000',-100,'2014-03-20 13:57:39.000',-100),
	 (35,NULL,'COVERDALE HOUSE','Coverdale House','2010-02-18 14:54:59.000',-100,'2010-02-18 14:55:28.000',-100),
	 (36,NULL,'CROWN COURT AT ALDERSGATE HOUSE','Aldersgate House','2021-02-22 18:10:56.000',-100,'2021-02-22 18:12:02.000',-100),
	 (37,NULL,'CROWN COURT AT HOLBORN','Crown Court at Holborn','2022-03-28 17:54:19.000',-100,'2022-03-28 17:54:43.000',-100),
	 (38,NULL,'CROWN COURT AT MONUMENT','Crown Court at Monument','2021-08-27 18:00:00.000',-100,'2021-08-27 18:00:30.000',-100),
	 (39,NULL,'CROWN COURT SITTING AT BARCLAY ROAD','Croydon Crown Court sitting at Barclay Road','2012-03-09 10:44:35.000',-100,'2012-03-09 10:53:12.000',-100),
	 (40,NULL,'CROYDON','Croydon','2012-03-09 10:41:44.000',-100,'2012-03-09 10:42:19.000',-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (41,NULL,'CROYDON JURYS INN','Croydon Jurys Inn','2021-03-08 17:42:08.000',-100,'2021-03-08 17:45:19.000',-100),
	 (42,NULL,'DERBY','Derby Combined Court Centre','2011-10-20 17:45:15.000',-100,'2011-10-20 17:45:28.000',-100),
	 (43,NULL,'DONCASTER','Doncaster','2012-01-11 12:07:17.000',-100,'2012-01-11 12:07:35.000',-100),
	 (44,NULL,'DONCASTER CROWN COURT','Doncaster Crown Court','2013-05-24 16:13:18.000',-100,'2013-05-24 16:14:39.000',-100),
	 (45,NULL,'DORCHESTER','Dorchester Combined Court Centre','2012-02-06 17:12:31.000',-100,'2012-02-06 17:13:12.000',-100),
	 (46,NULL,'DURHAM','Durham','2011-11-24 10:19:51.000',-100,'2011-11-24 10:40:51.000',-100),
	 (47,NULL,'EXETER','Exeter Combined Court Centre','2011-12-01 14:41:41.000',-100,'2011-12-01 14:42:01.000',-100),
	 (48,NULL,'FIELD HOUSE','Field House','2012-03-27 11:20:08.000',-100,'2012-03-27 11:20:30.000',-100),
	 (49,NULL,'GLOUCESTER','Gloucester','2012-01-18 11:29:30.000',-100,'2012-01-18 11:31:12.000',-100),
	 (50,NULL,'GREAT GRIMSBY','Great Grimsby Combined Court Centre','2011-12-14 12:25:07.000',-100,'2011-12-14 12:26:20.000',-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (51,NULL,'GUILDFORD','Guildford','2012-02-15 10:24:12.000',-100,'2012-02-15 10:24:33.000',-100),
	 (52,NULL,'GUILDFORD CROWN COURT SITE B','Guildford Crown Court Site B','2012-02-15 11:41:59.000',-100,'2012-02-15 11:43:42.000',-100),
	 (53,NULL,'HARROW','Harrow','2012-03-09 11:29:32.000',-100,'2012-03-09 11:29:46.000',-100),
	 (54,NULL,'HARROW CC AT WILLESDEN MC','Harrow CC at Willesden MC','2024-02-01 18:01:33.000',-100,'2024-02-01 18:02:21.000',-100),
	 (55,NULL,'HATTON CROSS','Hatton Cross','2023-12-01 18:06:49.000',-100,'2023-12-01 18:07:04.000',-100),
	 (56,NULL,'HEREFORD','Hereford','2012-02-23 13:01:26.000',-100,'2012-02-23 13:01:49.000',-100),
	 (57,NULL,'HOVE TRIAL CENTRE','Hove Trial Centre','2009-12-10 14:35:31.000',-100,'2009-12-10 14:37:12.000',-100),
	 (58,NULL,'HUNTINGDON LAW COURTS','Crown Court sitting at Huntingdon Law Courts','2011-11-10 15:17:46.000',-100,'2011-11-10 15:18:15.000',-100),
	 (59,NULL,'INNER LONDON','Inner London','2009-11-26 11:57:24.000',-100,'2009-11-26 11:59:50.000',-100),
	 (60,NULL,'INNER LONDON CC AT THE RCJ','Inner London CC at the RCJ','2020-12-11 09:35:43.000',-100,'2020-12-11 09:36:30.000',-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (61,NULL,'IPSWICH','Ipswitch','2012-01-11 11:47:10.000',-100,'2012-01-11 11:47:30.000',-100),
	 (62,NULL,'ISLEWORTH','Isleworth','2012-03-16 08:28:51.000',-100,'2012-03-16 08:30:27.000',-100),
	 (63,NULL,'KINGSTON UPON HULL','Kingston Upon Hull Combined Court Centre','2011-09-09 11:46:36.000',-100,'2011-09-09 11:47:42.000',-100),
	 (64,NULL,'KINGSTON UPON THAMES','Kingston','2012-03-23 10:27:50.000',-100,'2012-03-23 10:28:15.000',-100),
	 (65,NULL,'KNUTSFORD','Knutsford','2012-01-20 08:22:48.000',-100,'2012-01-20 08:23:15.000',-100),
	 (66,NULL,'LANCASTER','Lancaster','2011-12-07 09:56:25.000',-100,'2011-12-07 09:56:44.000',-100),
	 (67,NULL,'LANCASTER ASHTON HALL','Lancaster Ashton Hall','2020-12-10 18:02:09.000',-100,'2020-12-10 18:03:21.000',-100),
	 (68,NULL,'LEEDS','Leeds Combined Court Centre','2010-02-18 14:53:05.000',-100,'2010-02-18 14:53:38.000',-100),
	 (69,NULL,'LEEDS CLOTH HALL COURT','Leeds Cloth Hall Court','2021-08-20 17:11:03.000',-100,'2021-08-20 17:14:27.000',-100),
	 (70,NULL,'LEIC CROWN SITTING AT LEIC MAGS','Leic Crown Sitting at Leic Mags',TIMEZONE('UTC', '2024-03-28 17:58:14'),-100,TIMEZONE('UTC', '2024-03-28 18:01:10'),-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (71,NULL,'LEICESTER','Leicester','2011-10-07 09:15:06.000',-100,'2011-10-07 09:15:22.000',-100),
	 (72,NULL,'LEICS CROWN SITTING AT LOUGH','Leics Crown Sitting At Lough','2021-11-03 09:12:58.000',-100,'2021-11-03 11:36:46.000',-100),
	 (73,NULL,'LEONARDO HOTEL','Leonardo Hotel','2023-03-24 17:54:01.000',-100,'2023-03-24 17:54:28.000',-100),
	 (74,NULL,'LEWES CC SITTING AT CHICHESTER','Lewes CC Sitting at Chichester','2021-03-04 18:12:21.000',-100,'2021-03-04 18:16:25.000',-100),
	 (75,NULL,'LEWES COMBINED COURT','Lewes Combined Court Centre','2009-12-10 14:32:08.000',-100,'2009-12-10 14:32:57.000',-100),
	 (76,NULL,'LINC CROWN SITTING AT LINC MAGS','Linc Crown Sitting At Mags',TIMEZONE('UTC', '2024-06-27 10:57:52.419'),-100,TIMEZONE('UTC', '2024-03-28 18:03:10'),-100),
	 (77,NULL,'LINCOLN','Lincoln Combined Court Centre','2011-11-01 14:02:47.000',-100,'2011-11-01 14:02:57.000',-100),
	 (78,NULL,'LIVERPOOL','Liverpool Crown Court','2011-10-07 08:42:38.000',-100,'2011-10-07 08:43:05.000',-100),
	 (79,NULL,'LIVERPOOL COMMUNITY JUSTICE CENTRE','Liverpool CJC','2012-03-02 13:57:51.000',-100,'2012-03-02 13:58:29.000',-100),
	 (80,NULL,'LIVERPOOL HILTON HOTEL','Liverpool Hilton Hotel','2021-03-04 18:18:50.000',-100,'2021-03-04 18:19:26.000',-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (81,NULL,'LUTON','Luton','2011-10-20 17:44:14.000',-100,'2011-10-20 17:44:26.000',-100),
	 (82,NULL,'M40 J15 WARWICK HOTEL','M40 J15 Warwick Hotel','2021-10-21 17:54:13.000',-100,'2021-10-21 17:55:42.000',-100),
	 (83,NULL,'MAIDSTONE','Maidstone Combined Court','2012-01-26 13:36:36.000',-100,'2012-01-26 13:36:55.000',-100),
	 (84,NULL,'MAIDSTONE CC GREAT DANES HOTEL','Maidstone CC Great Danes Hotel','2021-05-18 18:02:34.000',-100,'2021-05-18 18:03:04.000',-100),
	 (85,NULL,'MANCHESTER','Manchester Crown Square','2011-11-10 15:22:18.000',-100,'2011-11-10 15:22:42.000',-100),
	 (86,NULL,'MANCHESTER HILTON DEANSGATE','Manchester Hilton Deansgate','2021-02-04 17:31:25.000',-100,'2021-02-04 17:32:26.000',-100),
	 (87,NULL,'MANCHESTER MINSHULL STREET','Manchester Minshull Street','2011-11-04 08:56:10.000',-100,'2011-11-04 08:56:31.000',-100),
	 (88,NULL,'MAPLE HOUSE','Birmingham Maple House','2021-02-23 17:12:53.000',-100,'2021-02-23 17:13:09.000',-100),
	 (89,NULL,'MERTHYR TYDFIL','Merthyr Tydfil Combined Court Centre','2012-02-14 16:46:22.000',-100,'2012-02-14 16:46:54.000',-100),
	 (90,NULL,'MINSHULL ST SITTING AT STOCKPORT','Minshull St Sitting at Stockport','2021-01-26 18:04:16.000',-100,'2021-01-26 18:06:35.000',-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (91,NULL,'MOLD','Mold','2012-01-26 13:32:09.000',-100,'2012-01-26 13:33:02.000',-100),
	 (92,NULL,'MOOT HALL','Newcastle Moot hall','2011-11-25 11:46:01.000',-100,'2011-11-25 11:46:19.000',-100),
	 (93,NULL,'NEWCASTLE UPON TYNE','Newcastle Combined Court Centre','2011-11-24 10:11:33.000',-100,'2011-11-24 10:14:24.000',-100),
	 (94,NULL,'NEWPORT CROWN COURT','Newport Gwent','2009-10-09 11:54:54.000',-100,'2009-10-09 11:55:13.000',-100),
	 (95,NULL,'NEWPORT, ISLE OF WIGHT','Newport IoW Combined Court Centre','2012-03-07 12:41:17.000',-100,'2012-03-07 12:46:00.000',-100),
	 (96,NULL,'NORTHAMPTON','Northampton Combined Court Centre','2012-03-07 12:37:31.000',-100,'2012-03-07 12:38:04.000',-100),
	 (97,NULL,'NORWICH','Norwich Combined Court Centre','2012-01-18 11:17:32.000',-100,'2012-01-18 11:39:26.000',-100),
	 (98,NULL,'NOTTINGHAM','Nottingham','2011-10-20 17:45:52.000',-100,'2011-10-20 17:46:05.000',-100),
	 (99,NULL,'NOTTINGHAM CC AT MERCURE HOTEL','Nottingham Mercure Hotel','2021-03-12 18:12:32.000',-100,'2021-03-12 18:13:06.000',-100),
	 (100,NULL,'OXFORD','Oxford Combined Court Centre','2012-02-15 10:02:38.000',-100,'2012-02-15 10:02:59.000',-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (101,NULL,'PETERBOROUGH','Peterborough Combined Court Centre','2011-11-10 14:44:57.000',-100,'2011-11-10 14:45:31.000',-100),
	 (102,NULL,'PETERBOROUGH KNIGHTS CHAMBER','Peterborough Knights Chamber','2020-08-24 18:06:04.000',-100,'2020-08-24 18:06:59.000',-100),
	 (103,NULL,'PETERBOROUGH MAGISTRATES COURT','Peterborough Magistrates Court','2013-03-22 11:08:47.000',-100,'2013-03-22 11:10:13.000',-100),
	 (104,NULL,'PLYMOUTH','Plymouth','2012-01-11 11:59:38.000',-100,'2012-01-11 12:04:59.000',-100),
	 (105,NULL,'PNE [INVINCIBLES PAVILION]','Preston Invincibles Pavilion','2021-04-07 18:16:02.000',-100,'2021-04-07 18:16:38.000',-100),
	 (106,NULL,'PNE [TOM FINNEY STAND]','Preston Tom Finney Stand','2021-04-06 18:05:04.000',-100,'2021-04-06 18:05:41.000',-100),
	 (107,NULL,'PORTSMOUTH','Portsmouth Combined Court Centre','2012-03-09 11:28:05.000',-100,'2012-03-09 11:28:34.000',-100),
	 (108,NULL,'PRESTON','Preston Combined Court Centre','2011-12-07 09:52:59.000',-100,'2011-12-07 09:53:23.000',-100),
	 (109,NULL,'PRESTON [THE SESSIONS HOUSE]','Preston Session House','2011-12-07 09:54:13.000',-100,'2011-12-07 11:44:27.000',-100),
	 (110,NULL,'PROSPERO HOUSE','Prospero House','2020-07-29 18:37:53.000',-100,'2020-07-29 18:38:32.000',-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (111,NULL,'RCJROLLSCH1','Rolls Building Court House 1','2011-09-21 14:00:08.000',-100,'2011-09-21 14:00:41.000',-100),
	 (112,NULL,'RCJROLLSCH2','Rolls Building Court House 2','2011-09-21 14:01:00.000',-100,'2011-09-21 14:01:17.000',-100),
	 (113,NULL,'RCJROLLSCH3','Rolls Building Court House 3','2011-09-21 14:01:31.000',-100,'2011-09-21 14:01:46.000',-100),
	 (114,NULL,'RCJROLLSCH4','Rolls Building Court House 4','2011-09-21 14:01:59.000',-100,'2011-09-21 14:02:14.000',-100),
	 (115,NULL,'READING','Reading Crown Court','2012-02-03 10:43:18.000',-100,'2012-02-03 10:43:34.000',-100),
	 (116,NULL,'READING MAGISTRATES COURT','Reading Magistrates Court','2012-02-03 10:39:36.000',-100,'2012-02-03 10:41:02.000',-100),
	 (117,NULL,'SALISBURY','Salisbury Combined Court Centre','2012-02-01 12:41:38.000',-100,'2012-02-01 12:42:14.000',-100),
	 (118,NULL,'SHREWSBURY','Shrewsbury','2011-11-22 18:14:11.000',-100,'2011-11-22 18:14:36.000',-100),
	 (119,NULL,'SNARESBROOK','Snaresbrook','2011-11-24 10:27:25.000',-100,'2011-11-24 10:27:44.000',-100),
	 (120,NULL,'SOUTHAMPTON','Southampton Combined Court Centre','2012-02-10 12:10:50.000',-100,'2012-02-10 12:11:28.000',-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (121,NULL,'SOUTHEND','Southend','2011-10-20 17:43:25.000',-100,'2011-10-20 17:43:38.000',-100),
	 (122,NULL,'SOUTHWARK','Southwark','2011-10-07 09:08:09.000',-100,'2011-10-07 09:08:27.000',-100),
	 (123,NULL,'STAFFORD','Stafford Combined Court Centre','2011-10-11 17:45:43.000',-100,'2011-10-11 17:47:32.000',-100),
	 (124,NULL,'ST ALBANS','St Albans','2011-10-14 10:22:16.000',-100,'2011-10-14 10:22:30.000',-100),
	 (125,NULL,'ST ALBANS MAGISTRATES COURT','Crown Court sitting at St Albans Magistrates Court','2011-10-14 11:13:24.000',-100,'2011-10-14 11:13:55.000',-100),
	 (126,NULL,'STOKE ON TRENT','Stoke on Trent Combined Court Centre','2012-01-24 14:16:12.000',-100,'2012-01-24 14:17:36.000',-100),
	 (127,NULL,'SWANSEA','Swansea','2012-02-23 14:06:07.000',-100,'2012-02-23 14:06:40.000',-100),
	 (128,NULL,'SWANSEA CIVIC CENTRE','Swansea Civic Centre','2020-08-12 18:10:25.000',-100,'2020-08-12 18:11:31.000',-100),
	 (129,NULL,'SWANSEA CROWN COURT','Swansea Crown Court (DL Cardiff)','2012-09-13 13:34:06.000',-100,'2012-09-13 13:36:28.000',-100),
	 (130,NULL,'SWINDON','Swindon Combined Court Centre','2011-09-06 11:32:04.000',-100,'2011-09-06 11:33:02.000',-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (131,NULL,'TAUNTON','Taunton','2011-09-14 12:07:50.000',-100,'2011-09-14 12:08:13.000',-100),
	 (132,NULL,'TAUNTON SITTING AT WORLE','Taunton sitting at Worle','2023-11-10 18:10:06.000',-100,'2023-11-10 18:10:31.000',-100),
	 (133,NULL,'TAYLOR HOUSE','Taylor House','2023-10-26 18:05:35.000',-100,'2023-10-26 18:06:13.000',-100),
	 (134,NULL,'TEESSIDE','Teesside Combined Court Centre','2011-12-01 14:53:01.000',-100,'2011-12-01 14:53:22.000',-100),
	 (135,NULL,'TEESSIDE MAGISTRATES COURT','Teesside Magistrates Court','2021-09-09 15:10:37.000',-100,'2021-09-09 15:11:37.000',-100),
	 (136,NULL,'THE CRIMINAL COURT, GUILDHALL','Swansea sitting at Guildhall','2012-02-23 14:07:46.000',-100,'2012-02-23 14:08:30.000',-100),
	 (137,NULL,'THE LAW COURTS WEST BAR SHEFFIELD','Sheffield Combined Court Centre','2011-10-14 09:28:50.000',-100,'2011-10-14 09:42:25.000',-100),
	 (138,NULL,'THE LOWRY THEATRE','The Lowry Theatre','2020-09-18 18:16:56.000',-100,'2020-09-18 18:17:18.000',-100),
	 (139,NULL,'TRURO','Truro Combined Court Centre','2011-12-14 12:29:19.000',-100,'2011-12-14 12:29:32.000',-100),
	 (140,NULL,'WARRINGTON','Warrington Combined Court Centre','2012-01-20 08:29:44.000',-100,'2012-01-20 08:30:21.000',-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (141,NULL,'WARWICK','Warwick Combined Court Centre','2012-03-16 08:27:11.000',-100,'2012-03-16 08:27:37.000',-100),
	 (142,NULL,'WEST MIDLANDS COURT CENTRE','West Midlands Courth Centre','2011-09-29 17:58:42.000',-100,'2011-09-29 17:59:16.000',-100),
	 (143,NULL,'WINCHESTER','Winchester Combined Court Centre','2012-02-03 10:49:37.000',-100,'2012-02-03 10:50:42.000',-100),
	 (144,NULL,'WOLVERHAMPTON','Wolverhampton Combined Court Centre','2011-09-29 17:49:41.000',-100,'2011-09-29 17:50:43.000',-100),
	 (145,NULL,'WOLVERHAMPTON CC AT PARK HALL','Wolverhampton CC at Park Hall','2021-02-25 17:55:55.000',-100,'2021-02-25 17:57:44.000',-100),
	 (146,NULL,'WOLVERHAMPTON MAGISTRATES COURT','Wolverhampton Magistrates Court','2011-10-04 15:36:14.000',-100,'2011-10-04 15:36:54.000',-100),
	 (147,NULL,'WOOD GREEN','Wood Green','2011-10-14 10:19:41.000',-100,'2011-10-14 10:21:16.000',-100),
	 (148,NULL,'WOOD GREEN CC AT HENDON MC','Wood Green at Hendon MC','2021-03-22 18:11:11.000',-100,'2021-03-22 18:11:38.000',-100),
	 (149,NULL,'WOOLWICH','Woolwich','2011-09-23 14:31:07.000',-100,'2011-09-23 14:31:50.000',-100),
	 (150,NULL,'WORCESTER SHIRE HALL','Worcester Combined Court Centre','2012-02-23 12:59:15.000',-100,'2012-02-23 12:59:51.000',-100);
INSERT INTO courthouse (cth_id,courthouse_code,courthouse_name,display_name,created_ts,created_by,last_modified_ts,last_modified_by) VALUES
	 (151,NULL,'YORK','York','2011-11-01 14:01:47.000',-100,'2011-11-01 14:02:04.000',-100);


-- Associate courthouse to region
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (10,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (24,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (36,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (37,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (38,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (41,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (51,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (52,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (53,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (54,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (59,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (60,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (62,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (64,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (73,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (110,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (119,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (122,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (147,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (148,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (149,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (1,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (5,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (8,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (9,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (33,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (34,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (42,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (48,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (56,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (55,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (71,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (70,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (72,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (76,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (77,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (82,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (88,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (96,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (98,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (99,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (118,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (123,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (126,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (133,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (141,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (142,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (144,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (145,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (146,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (150,4);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (14,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (35,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (43,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (44,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (46,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (50,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (63,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (68,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (69,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (92,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (93,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (134,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (135,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (137,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (151,5);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (6,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (11,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (12,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (17,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (22,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (27,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (28,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (29,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (30,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (66,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (67,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (78,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (79,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (80,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (85,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (86,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (87,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (90,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (105,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (106,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (108,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (109,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (138,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (140,6);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (2,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (3,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (7,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (15,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (19,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (20,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (25,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (26,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (31,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (39,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (40,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (57,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (58,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (61,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (74,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (75,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (81,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (83,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (84,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (97,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (100,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (101,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (102,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (103,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (115,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (116,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (121,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (124,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (125,2);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (4,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (13,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (16,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (32,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (45,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (47,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (49,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (95,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (104,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (107,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (117,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (120,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (130,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (131,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (132,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (139,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (143,3);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (18,7);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (21,7);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (23,7);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (89,7);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (91,7);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (94,7);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (127,7);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (128,7);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (129,7);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (136,7);

-- These courthouses need a Region mapped.
--  See email April 18, 2024 from Janet Healey - map to London region
--65  'KNUTSFORD'
--111 'RCJROLLSCH1'
--112 'RCJROLLSCH2'
--113 'RCJROLLSCH3'
--114 'RCJROLLSCH4'

INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (65,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (111,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (112,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (113,1);
INSERT INTO courthouse_region_ae (cth_id, reg_id) VALUES (114,1);

