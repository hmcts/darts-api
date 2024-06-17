package uk.gov.hmcts.darts.casedocument.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.casedocument.template.CourtCaseDocument;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.darts.test.common.data.CaseTestData.someMinimalCase;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.createCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createDefendantForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.createEventWith;
import static uk.gov.hmcts.darts.test.common.data.HearingTestData.createHearingWithDefaults;
import static uk.gov.hmcts.darts.test.common.data.JudgeTestData.createJudgeWithName;

public class CaseDocumentServiceIntTest extends IntegrationBase {

    @Autowired
    SecurityGroupRepository securityGroupRepository;
    @Autowired
    UserAccountRepository userAccountRepository;
    CourthouseEntity swanseaCourthouse;
    CourtCaseEntity courtCase;

    @Autowired
    CaseDocumentService caseDocumentService;

    @BeforeEach
    void setupData() {
        swanseaCourthouse = createCourthouse("SWANSEA");

        UserAccountEntity user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        courtCase = someMinimalCase((createdCase -> {
            createdCase.setCourthouse(swanseaCourthouse);
            createdCase.setCreatedBy(user);
            createdCase.setLastModifiedBy(user);
        }));
        courtCase.setDefendantList(List.of(createDefendantForCaseWithName(courtCase, "Defendant1")));
        JudgeEntity judge = createJudgeWithName("aJudge");
        CourtroomEntity courtroom1 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom1");
        HearingEntity hearing1a = createHearingWithDefaults(courtCase, courtroom1, LocalDate.of(2023, 5, 20), judge);
        HearingEntity hearing1b = createHearingWithDefaults(courtCase, courtroom1, LocalDate.of(2023, 5, 21), judge);
        JudgeEntity judge3a = createJudgeWithName("Judge3a");
        hearing1a.addJudge(judge3a);
        dartsDatabase.saveAll(hearing1a, hearing1b);

        EventEntity event4a = createEventWith("eventName", "event4a", hearing1a, OffsetDateTime.now());
        dartsDatabase.saveAll(event4a);

        userAccountRepository.save(user);
    }

    @Test
    void testGenerationOfCaseDocument() {

        CourtCaseDocument document = caseDocumentService.generateCaseDocument(courtCase.getId());

        assertAll(
            "Grouped assertions of Court Case",
            () -> assertThat(document.getId()).isNotNull().isEqualTo(courtCase.getId()),
            () -> assertThat(document.getCreatedBy()).isNotNull().isEqualTo(courtCase.getCreatedBy().getId()),
            () -> assertThat(document.getCaseNumber()).isNotNull().isEqualTo(courtCase.getCaseNumber())
        );
    }

}
