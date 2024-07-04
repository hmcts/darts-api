-- This script deletes data that conflicts with the constraints imposed with v70 of the datamodel.

DELETE
FROM external_object_directory
WHERE (ado_id is null and cad_id is null and med_id is null and trd_id is null);

DELETE
FROM prosecutor
WHERE prosecutor.prosecutor_name IN (SELECT DISTINCT prosecutor_name
                                     FROM prosecutor
                                     GROUP BY cas_id, prosecutor_name
                                     HAVING COUNT(*) > 1);

DELETE
FROM defence
WHERE defence.defence_name IN (SELECT DISTINCT defence_name
                               FROM defence
                               GROUP BY cas_id, defence_name
                               HAVING COUNT(*) > 1);

DELETE
FROM defendant
WHERE defendant.defendant_name IN (SELECT DISTINCT defendant_name
                                   FROM defendant
                                   GROUP BY cas_id, defendant_name
                                   HAVING COUNT(*) > 1);
