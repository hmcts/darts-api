package uk.gov.hmcts.darts.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.HearingRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HearingService {

    private final HearingRepository hearingRepository;
    private final CaseService caseService;
    private final CourtroomService courtroomService;
    private final AuthorisationApi authorisationApi;

    @Transactional
    public HearingEntity createHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                       UserAccountEntity userAccount, MediaEntity mediaEntity) {
        final CourtCaseEntity courtCase = caseService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
        final CourtroomEntity courtroom = courtroomService.retrieveOrCreateCourtroom(courtCase.getCourthouse(), courtroomName, userAccount);
        HearingEntity hearing = new HearingEntity();

        hearing.setCourtCase(courtCase);
        hearing.setCourtroom(courtroom);
        hearing.setHearingDate(hearingDate.toLocalDate());
        hearing.setScheduledStartTime(hearingDate.toLocalTime().withNano(0));
        hearing.setNew(true);
        hearing.setHearingIsActual(false);
        hearing.setCreatedBy(userAccount);
        hearing.setLastModifiedBy(userAccount);
        if (mediaEntity != null) {
            hearing.getMediaList().add(mediaEntity);
        }
        hearingRepository.saveAndFlush(hearing);

        return hearing;
    }

    @Transactional
    public HearingEntity setHearingLastDateModifiedBy(final HearingEntity hearingEntity, final UserAccountEntity userAccountEntity) {
        hearingEntity.setLastModifiedBy(userAccountEntity);
        hearingRepository.saveAndFlush(hearingEntity);
        return hearingEntity;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate) {

        UserAccountEntity userAccount = authorisationApi.getCurrentUser();

        Optional<HearingEntity> foundHearing = hearingRepository.findHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate.toLocalDate()
        );

        return foundHearing.map(hearingEntity -> setHearingLastDateModifiedBy(hearingEntity, userAccount))
            .orElseGet(() -> createHearing(
                courthouseName,
                courtroomName,
                caseNumber,
                hearingDate,
                userAccount,
                null
            ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                 UserAccountEntity userAccount) {
        Optional<HearingEntity> foundHearing = hearingRepository.findHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate.toLocalDate()
        );

        return foundHearing.map(hearingEntity -> setHearingLastDateModifiedBy(hearingEntity, userAccount))
            .orElseGet(() -> createHearing(
                courthouseName,
                courtroomName,
                caseNumber,
                hearingDate,
                userAccount,
                null
            ));
    }

    @Transactional
    public HearingEntity retrieveOrCreateHearingWithMedia(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                          UserAccountEntity userAccount, MediaEntity mediaEntity) {
        Optional<HearingEntity> foundHearing = hearingRepository.findHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate.toLocalDate()
        );

        return foundHearing.map(hearingEntity -> setHearingLastDateModifiedBy(hearingEntity, userAccount))
            .orElseGet(() -> createHearing(
                courthouseName,
                courtroomName,
                caseNumber,
                hearingDate,
                userAccount,
                mediaEntity
            ));
    }
}