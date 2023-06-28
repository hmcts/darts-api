package uk.gov.hmcts.darts.common.api.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.api.CommonApi;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.CommonCourthouseRepository;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.service.CommonTransactionalService;

import java.text.MessageFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommonApiImpl implements CommonApi {

    private final CourtroomRepository courtroomRepository;
    private final CommonCourthouseRepository courthouseRepository;
    private final CommonTransactionalService commonTransactionalService;

    @Override
    public CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName) {
        CourtroomEntity courtroom = courtroomRepository.findByNames(courthouseName, courtroomName);
        if (courtroom == null) {
            //make sure courthouse exists
            CourthouseEntity courthouse = courthouseRepository.findByCourthouseNameIgnoreCase(courthouseName);
            if (courthouse == null) {
                //Courthouses need to be created manually in the screens. throw an error.
                String message = MessageFormat.format("Courthouse ''{0}'' not found.", courthouseName);
                log.error(message);
                throw new DartsException(message);
            } else {
                courtroom = commonTransactionalService.createCourtroom(courthouse, courtroomName);
            }
        }
        return courtroom;
    }
}
