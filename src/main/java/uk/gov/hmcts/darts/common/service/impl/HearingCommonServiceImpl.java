package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.CaseCommonService;
import uk.gov.hmcts.darts.common.service.CourtroomCommonService;
import uk.gov.hmcts.darts.common.service.HearingCommonService;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class HearingCommonServiceImpl implements HearingCommonService {

    private final HearingRepository hearingRepository;
    private final CaseCommonService caseCommonService;
    private final CourtroomCommonService courtroomCommonService;

    @Override
    @Transactional
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                 UserAccountEntity userAccount) {
        Optional<HearingEntity> foundHearing = hearingRepository.findHearing(courthouseName, courtroomName, caseNumber, hearingDate.toLocalDate());

        return foundHearing.orElseGet(() -> createHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount, null));
    }

    @Override
    @Transactional
    @Deprecated(since = "This method is used only in tests and will be removed in future.")
    public HearingEntity retrieveOrCreateHearingWithMedia(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                                          UserAccountEntity userAccount, MediaEntity mediaEntity) {
        Optional<HearingEntity> foundHearing = hearingRepository.findHearing(courthouseName, courtroomName, caseNumber, hearingDate.toLocalDate());
        return foundHearing.orElseGet(() -> createHearing(courthouseName, courtroomName, caseNumber, hearingDate, userAccount, mediaEntity));
    }

    private HearingEntity createHearing(String courthouseName, String courtroomName, String caseNumber, LocalDateTime hearingDate,
                                        UserAccountEntity userAccount, MediaEntity mediaEntity) {
        final CourtCaseEntity courtCase = caseCommonService.retrieveOrCreateCase(courthouseName, caseNumber, userAccount);
        final CourtroomEntity courtroom = courtroomCommonService.retrieveOrCreateCourtroom(courtCase.getCourthouse(), courtroomName, userAccount);

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
        return hearingRepository.saveAndFlush(hearing);
    }
}
