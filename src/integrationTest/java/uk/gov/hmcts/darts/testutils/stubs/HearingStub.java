package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.HearingSubStringQueryEnum;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentSubStringQueryEnum;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;

@Component
@RequiredArgsConstructor
@Deprecated
public class HearingStub {

    private final CourthouseStub courthouseStub;
    private final CourtroomStub courtroomStub;
    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final UserAccountStub userAccountStub;
    private final UserAccountRepository userAccountRepository;
    private final CourtCaseStub courtCaseStub;
    private final HearingStubComposable hearingStubShare;

    public HearingEntity createHearing(String courthouseName, String courtroomName, String caseNumber,
                                       LocalDateTime hearingDate) {
        return hearingStubShare.createHearing(courthouseName, courtroomName, caseNumber, hearingDate, courthouseStub, userAccountStub);
    }

    public HearingEntity createHearing(CourtCaseEntity courtCase, CourtroomEntity courtroomEntity,
                                       LocalDateTime hearingDate) {
        return createHearing(courtCase.getCourthouse().getCourthouseName(), courtroomEntity.getName(), courtCase.getCaseNumber(), hearingDate);

    }

    public HearingEntity createHearingWithMedia(String courthouseName, String courtroomName, String caseNumber,
                                                LocalDateTime hearingDate, MediaEntity mediaEntity) {
        courthouseStub.createCourthouseUnlessExists(courthouseName);
        return retrieveCoreObjectService.retrieveOrCreateHearingWithMedia(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate,
            userAccountStub.getSystemUserAccountEntity(),
            mediaEntity
        );
    }

    public HearingEntity createMinimalHearing() {
        CourthouseEntity minimalCourthouse = courthouseStub.createMinimalCourthouse();
        return createHearing(minimalCourthouse.getCourthouseName(), "1", "caseNumber1", LocalDateTime.of(2020, 10, 1, 10, 0, 0));
    }

    /**
     * generates test data. The following will be used for generation:-
     * Unique owner for each hearing record.  See {@link HearingSubStringQueryEnum} for prefix and postfix
     * Unique court house with unique name for each hearing record.
     * See {@link HearingSubStringQueryEnum} for prefix and postfix
     * Unique case number with unique case number for each hearing record.
     * See {@link HearingSubStringQueryEnum} for prefix and postfix
     * Unique hearing date starting with today with an incrementing day for each subsequent hearing record
     *
     * @param count The number of hearing objects that are to be generated
     * @return The list of generated hearings in chronological order
     */
    @Transactional
    public List<HearingEntity> generateHearings(int count) {
        List<HearingEntity> retHearingList = new ArrayList<>();
        OffsetDateTime requestedDate = now(UTC);
        LocalDateTime hearingDate = LocalDateTime.now(UTC);

        for (int hearingCount = 0; hearingCount < count; hearingCount++) {
            UserAccountEntity owner = userAccountStub.createSystemUserAccount(
                HearingSubStringQueryEnum.REQUESTED_BY.getQueryString(Integer.toString(hearingCount)));

            CourtroomEntity courtroomEntity = courtroomStub.createCourtroomUnlessExists(
                TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE.getQueryString(Integer.toString(hearingCount)),
                TranscriptionDocumentSubStringQueryEnum.COURT_HOUSE
                    .getQueryString(Integer.toString(hearingCount)), userAccountRepository.getReferenceById(0));

            CourtCaseEntity caseEntity = courtCaseStub.createAndSaveMinimalCourtCase(HearingSubStringQueryEnum.CASE_NUMBER.getQueryString(
                Integer.toString(hearingCount)), courtroomEntity.getCourthouse().getId());

            HearingEntity hearingEntity = retrieveCoreObjectService.retrieveOrCreateHearing(
                courtroomEntity.getCourthouse().getCourthouseName(),
                courtroomEntity.getName(),
                caseEntity.getCaseNumber(),
                hearingDate,
                owner);

            hearingDate = hearingDate.plusDays(1);
            requestedDate = requestedDate.plusDays(1);
            retHearingList.add(hearingEntity);
        }

        return retHearingList;
    }
}