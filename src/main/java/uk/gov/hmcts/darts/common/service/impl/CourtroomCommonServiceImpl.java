package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.service.CourthouseCommonService;
import uk.gov.hmcts.darts.common.service.CourtroomCommonService;

import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CourtroomCommonServiceImpl implements CourtroomCommonService {

    private final CourtroomRepository courtroomRepository;
    private final CourthouseCommonService courthouseCommonService;

    @Override
    @Transactional
    public CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName, UserAccountEntity userAccount) {
        final String courtroomNameUpper = courtroomName.toUpperCase(Locale.ROOT);
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByNameAndId(courthouse.getId(), courtroomNameUpper);
        return foundCourtroom.orElseGet(() -> createCourtroom(courthouse, courtroomNameUpper, userAccount));
    }

    @Override
    @Transactional
    public CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName, UserAccountEntity userAccount) {
        String courtroomNameUpper = courtroomName.toUpperCase(Locale.ROOT);
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByCourthouseNameAndCourtroomName(courthouseName, courtroomNameUpper);
        if (foundCourtroom.isPresent()) {
            return foundCourtroom.get();
        }

        CourthouseEntity courthouse = courthouseCommonService.retrieveCourthouse(courthouseName);
        return createCourtroom(courthouse, courtroomNameUpper, userAccount);
    }

    private CourtroomEntity createCourtroom(CourthouseEntity courthouse, String courtroomName, UserAccountEntity userAccount) {
        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(courtroomName);
        courtroom.setCreatedBy(userAccount);
        return courtroomRepository.saveAndFlush(courtroom);
    }
}