INSERT INTO transcription_urgency
  (tru_id, description, display_state)
VALUES (7, 'Up to 2 working days', true);
UPDATE transcription_urgency
set display_state= true
WHERE tru_id = 3;
