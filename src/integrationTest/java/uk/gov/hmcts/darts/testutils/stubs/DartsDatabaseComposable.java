package uk.gov.hmcts.darts.testutils.stubs;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DartsDatabaseComposable {

    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final UserAccountRepository userAccountRepository;
    private final HearingRepository hearingRepository;

    public HearingEntity createHearing(CourthouseStubComposable courthouseStubComposable, String courthouseName, String courtroomName, String caseNumber,
                                       LocalDateTime hearingDate) {
        createCourthouseUnlessExists(courthouseStubComposable, courthouseName);
        return retrieveCoreObjectService.retrieveOrCreateHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate,
            userAccountRepository.getReferenceById(0)
        );
    }

    public HearingEntity givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
        CourthouseStubComposable courthouseStubComposable, String caseNumber, String courthouseName, String courtroomName, LocalDateTime hearingDate) {
        createCourthouseUnlessExists(courthouseStubComposable, courthouseName);
        HearingEntity hearing = retrieveCoreObjectService.retrieveOrCreateHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate,
            userAccountRepository.getReferenceById(0)
        );
        hearing.setHearingIsActual(true);
        hearing.addJudge(createSimpleJudge(caseNumber + "judge1"));
        return hearingRepository.saveAndFlush(hearing);
    }

    public JudgeEntity createSimpleJudge(String name) {
        return retrieveCoreObjectService.retrieveOrCreateJudge(name, userAccountRepository.getReferenceById(0));
    }

    public CourthouseEntity createCourthouseUnlessExists(CourthouseStubComposable courthouseStubComposable, String courthouseName) {
        return courthouseStubComposable.createCourthouseUnlessExists(courthouseName);
    }
}