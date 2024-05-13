package uk.gov.hmcts.darts.usermanagement.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionWorkflowRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;

import java.time.LocalDateTime;

@AutoConfigureMockMvc
public class TranscriptionIntTest extends IntegrationBase {
    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockBean
    private UserIdentity userIdentity;

    private TranscriptionStub transcriptionStub;

    private UserAccountRepository userAccountRepository;

    @Test
    void getTransactionsForUserBeyondOrEqualToDate() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        HearingEntity headerEntity = dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName()
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        TranscriptionEntity transcriptionEntity = transcriptionStub.createTranscription(headerEntity);

        userAccountRepository
        transcriptionStub.createTranscriptionWorkflowEntity(transcriptionEntity,
                                                            );
    }
}