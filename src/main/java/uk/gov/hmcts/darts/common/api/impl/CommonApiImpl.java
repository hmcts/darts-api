package uk.gov.hmcts.darts.common.api.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.api.CommonApi;
import uk.gov.hmcts.darts.common.entity.Courthouse;
import uk.gov.hmcts.darts.common.entity.Courtroom;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.CommonCourthouseRepository;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;

import java.text.MessageFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommonApiImpl implements CommonApi {

    private final CourtroomRepository courtroomRepository;
    private final CommonCourthouseRepository courthouseRepository;

    @Override
    public Courtroom retrieveOrCreateCourtroom(String courthouseName, String courtroomName) {
        Courtroom courtroom = courtroomRepository.findByNames(courthouseName, courtroomName);
        if (courtroom == null) {
            //make sure courthouse exists
            Courthouse courthouse = courthouseRepository.findByCourthouseNameIgnoreCase(courthouseName);
            if (courthouse == null) {
                //Courthouses need to be created manually in the screens. throw an error.
                String message = MessageFormat.format("Courthouse ''{0}'' not found.", courthouseName);
                log.error(message);
                throw new DartsException(message);
            } else {
                //create the courtroom
                courtroom = new Courtroom();
                courtroom.setName(courtroomName.toUpperCase(Locale.ROOT));
                courtroom.setCourthouse(courthouse);
                courtroomRepository.saveAndFlush(courtroom);
            }
        }
        return courtroom;
    }
}
