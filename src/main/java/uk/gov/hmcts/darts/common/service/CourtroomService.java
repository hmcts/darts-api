package uk.gov.hmcts.darts.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourtroomService {

    private final CourtroomRepository courtroomRepository;
    private final CourthouseService courthouseService;

    @Transactional
    public CourtroomEntity createCourtroom(CourthouseEntity courthouse, String courtroomName, UserAccountEntity userAccount) {
        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(courtroomName);
        courtroom.setCreatedBy(userAccount);
        courtroomRepository.saveAndFlush(courtroom);
        return courtroom;
    }

    @Transactional
    public CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName, UserAccountEntity userAccount) {
        courtroomName = courtroomName.toUpperCase(Locale.ROOT);
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByCourthouseNameAndCourtroomName(
            courthouseName,
            courtroomName
        );
        if (foundCourtroom.isPresent()) {
            return foundCourtroom.get();
        }

        CourthouseEntity courthouse = courthouseService.retrieveCourthouse(courthouseName);
        return createCourtroom(courthouse, courtroomName, userAccount);
    }

    @Transactional
    public CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName, UserAccountEntity userAccount) {
        courtroomName = courtroomName.toUpperCase(Locale.ROOT);
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByNameAndId(
            courthouse.getId(),
            courtroomName
        );
        if (foundCourtroom.isPresent()) {
            return foundCourtroom.get();
        } else {
            return createCourtroom(courthouse, courtroomName, userAccount);
        }
    }
}