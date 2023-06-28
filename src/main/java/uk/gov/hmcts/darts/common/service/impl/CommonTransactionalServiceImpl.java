package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.service.CommonTransactionalService;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommonTransactionalServiceImpl implements CommonTransactionalService {

    private final CourtroomRepository courtroomRepository;

    @Override
    @Transactional
    public CourtroomEntity createCourtroom(CourthouseEntity courthouse, String courtroomName) {
        CourtroomEntity courtroom = new CourtroomEntity();

        try {
            courtroom.setName(courtroomName.toUpperCase(Locale.ROOT));
            courtroom.setCourthouse(courthouse);
            courtroomRepository.saveAndFlush(courtroom);
        } catch (DataIntegrityViolationException e) {
            log.warn(
                "Trying to create a courtroom that already exists. courthouse={}, courtroom={}.",
                courthouse.getCourthouseName(),
                courtroom,
                e
            );
            courtroom = courtroomRepository.findByNameAndId(courthouse.getId(), courtroomName);
        }

        return courtroom;
    }
}
