-- replace tempstorage with dets
UPDATE darts.external_location_type SET elt_description = 'dets' where elt_id = 4;