package uk.gov.hmcts.darts.usermanagement.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityGroupEnum;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.data.UserAccountTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.SecurityGroupStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;
import uk.gov.hmcts.darts.usermanagement.model.Problem;
import uk.gov.hmcts.darts.usermanagement.model.UserPatch;
import uk.gov.hmcts.darts.usermanagement.model.UserWithIdAndTimestamps;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class UserControllerTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/users/";

    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DartsDatabaseStub dartsDatabaseStub;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockBean
    private UserIdentity userIdentity;

    @Autowired
    private SecurityGroupRepository securityGroupRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private SecurityGroupStub securityGroupStub;

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";
    private static final OffsetDateTime YESTERDAY = now(UTC).minusDays(1).withHour(9).withMinute(0)
        .withSecond(0).withNano(0);

    @Test
    void testDeactivateModifyWithSuperAdmin() throws Exception {
        superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountEntity = userAccountRepository.save(userAccountEntity);

        dartsDatabase.addUserToGroup(userAccountEntity, SecurityGroupEnum.SUPER_ADMIN);

        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME));

        var courtCase = authorisationStub.getCourtCaseEntity();
        TranscriptionEntity transcription
            = dartsDatabase.getTranscriptionStub().createAndSaveWithTranscriberTranscription(userAccountEntity, courtCase, hearingEntity,  YESTERDAY, false);

        // now run the test to disable the user
        UserPatch userPatch = new UserPatch();
        userPatch.setActive(false);
        userPatch.setDescription("");
        List<TranscriptionWorkflowEntity> workflowEntityBefore
            = dartsDatabase.getTranscriptionWorkflowRepository().findByTranscriptionOrderByWorkflowTimestampDesc(transcription);
        Assertions.assertFalse(containsApprovedWorkflow(workflowEntityBefore));

        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT_URL + userAccountEntity.getId())
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(userPatch)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        Optional<UserAccountEntity> fndUserIdentity = dartsDatabase.getUserAccountRepository().findById(userAccountEntity.getId());
        Assertions.assertTrue(fndUserIdentity.isPresent());

        Assertions.assertFalse(securityGroupStub.isPartOfAnySecurityGroup(fndUserIdentity.get().getId()));

        objectMapper.registerModule(new JavaTimeModule());

        UserWithIdAndTimestamps userWithIdAndTimestamps = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                                                              UserWithIdAndTimestamps.class);

        List<Integer> rolledBackTranscription = userWithIdAndTimestamps.getRolledBackTranscriptRequests();

        List<TranscriptionWorkflowEntity> workflowEntityAfter
            = dartsDatabase.getTranscriptionWorkflowRepository().findByTranscriptionOrderByWorkflowTimestampDesc(transcription);

        Assertions.assertEquals(1, rolledBackTranscription.size());
        Assertions.assertEquals(transcription.getId(), rolledBackTranscription.get(0));
        Assertions.assertEquals(workflowEntityBefore.size() + 1, workflowEntityAfter.size());
        Assertions.assertTrue(containsApprovedWorkflow(workflowEntityAfter));
    }

    @Test
    void testDeactivateUserWithSuperUser() throws Exception {
        superAdminUserStub.givenSystemUserIsAuthorised(userIdentity);

        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountEntity = userAccountRepository.save(userAccountEntity);

        // add user to the super user group
        Optional<SecurityGroupEntity> groupEntity
            = securityGroupRepository.findByGroupNameIgnoreCase(SecurityGroupEnum.SUPER_USER.getName());
        Set<UserAccountEntity> entitiesSet = new HashSet<>();
        entitiesSet.add(userAccountEntity);

        groupEntity.get().setUsers(entitiesSet);
        dartsDatabase.addUserToGroup(userAccountEntity, groupEntity.get());

        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME));

        var courtCase = authorisationStub.getCourtCaseEntity();
        TranscriptionEntity transcription
            = dartsDatabase.getTranscriptionStub().createAndSaveWithTranscriberTranscription(userAccountEntity, courtCase, hearingEntity,  YESTERDAY, false);

        // now run the test to disable the user
        UserPatch userPatch = new UserPatch();
        userPatch.setActive(false);

        List<TranscriptionWorkflowEntity> workflowEntityBefore
            = dartsDatabase.getTranscriptionWorkflowRepository().findByTranscriptionOrderByWorkflowTimestampDesc(transcription);
        Assertions.assertFalse(containsApprovedWorkflow(workflowEntityBefore));

        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT_URL + userAccountEntity.getId())
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(userPatch)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        Optional<UserAccountEntity> fndUserIdentity = dartsDatabase.getUserAccountRepository().findById(userAccountEntity.getId());
        Assertions.assertTrue(fndUserIdentity.isPresent());

        Assertions.assertFalse(securityGroupStub.isPartOfAnySecurityGroup(fndUserIdentity.get().getId()));

        objectMapper.registerModule(new JavaTimeModule());

        UserWithIdAndTimestamps userWithIdAndTimestamps = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                                                                           UserWithIdAndTimestamps.class);

        List<Integer> rolledBackTranscription = userWithIdAndTimestamps.getRolledBackTranscriptRequests();

        List<TranscriptionWorkflowEntity> workflowEntityAfter
            = dartsDatabase.getTranscriptionWorkflowRepository().findByTranscriptionOrderByWorkflowTimestampDesc(transcription);

        Assertions.assertEquals(1, rolledBackTranscription.size());
        Assertions.assertEquals(transcription.getId(), rolledBackTranscription.get(0));
        Assertions.assertEquals(workflowEntityBefore.size() + 1, workflowEntityAfter.size());
        Assertions.assertTrue(containsApprovedWorkflow(workflowEntityAfter));
    }

    @Test
    void testActivateModifyUserWithSuperAdmin() throws Exception {
        superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountEntity = userAccountRepository.save(userAccountEntity);
        userAccountEntity.setActive(false);
        userAccountEntity.setEmailAddress("test@hmcts.net");
        userAccountEntity.setUserFullName("full name");

        // add user to the super admin group
        Optional<SecurityGroupEntity> groupEntity
            = securityGroupRepository.findByGroupNameIgnoreCase(SecurityGroupEnum.SUPER_ADMIN.getName());
        Set<UserAccountEntity> entitiesSet = new HashSet<>();
        entitiesSet.add(userAccountEntity);

        groupEntity.get().setUsers(entitiesSet);
        dartsDatabase.addUserToGroup(userAccountEntity, groupEntity.get());

        HearingEntity hearingEntity
            = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME));

        TranscriptionEntity transcription
            = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity, userAccountEntity, TranscriptionStatusEnum.WITH_TRANSCRIBER);

        // now run the test to activate
        UserPatch userPatch = new UserPatch();
        userPatch.setDescription("test");
        userPatch.setActive(true);

        List<TranscriptionWorkflowEntity> workflowEntityBefore
            = dartsDatabase.getTranscriptionWorkflowRepository().findByTranscriptionOrderByWorkflowTimestampDesc(transcription);

        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT_URL + userAccountEntity.getId())
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(userPatch)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        Optional<UserAccountEntity> fndUserIdentity
            = dartsDatabase.getUserAccountRepository().findById(userAccountEntity.getId());
        Assertions.assertTrue(fndUserIdentity.isPresent());

        objectMapper.registerModule(new JavaTimeModule());

        UserWithIdAndTimestamps userWithIdAndTimestamps = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                                                                           UserWithIdAndTimestamps.class);

        List<Integer> rolledBackTranscription = userWithIdAndTimestamps.getRolledBackTranscriptRequests();

        List<TranscriptionWorkflowEntity> workflowEntityAfter
            = dartsDatabase.getTranscriptionWorkflowRepository().findByTranscriptionOrderByWorkflowTimestampDesc(transcription);

        Assertions.assertNull(rolledBackTranscription);
        Assertions.assertEquals(workflowEntityBefore.size(), workflowEntityAfter.size());
    }

    @Test
    void testActivateModifyUserWithSuperAdminAndFailWithNoFullName() throws Exception {
        superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountEntity.setEmailAddress("test@hmcts.net");
        userAccountEntity.setUserFullName(null);
        userAccountEntity.setActive(false);
        userAccountEntity = userAccountRepository.save(userAccountEntity);

        // add user to the super admin group
        Optional<SecurityGroupEntity> groupEntity
            = securityGroupRepository.findByGroupNameIgnoreCase(SecurityGroupEnum.SUPER_ADMIN.getName());
        Set<UserAccountEntity> entitiesSet = new HashSet<>();
        entitiesSet.add(userAccountEntity);

        groupEntity.get().setUsers(entitiesSet);
        dartsDatabase.addUserToGroup(userAccountEntity, groupEntity.get());

        HearingEntity hearingEntity
            = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME));

        dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity, userAccountEntity, TranscriptionStatusEnum.WITH_TRANSCRIBER);

        // now run the test to disable the user
        UserPatch userPatch = new UserPatch();
        userPatch.setDescription("test");
        userPatch.setActive(true);

        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT_URL + userAccountEntity.getId())
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(userPatch)))
            .andExpect(status().isConflict())
            .andReturn();

        uk.gov.hmcts.darts.transcriptions.model.Problem failureResponse = objectMapper
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(
            mvcResult.getResponse().getContentAsString(),
            uk.gov.hmcts.darts.transcriptions.model.Problem.class
        );

        // assert the failure response
        Assertions.assertEquals(UserManagementError.USER_ACTIVATION_FULLNAME_OR_EMAIL_VIOLATION.getType(), failureResponse.getType());

    }

    @Test
    void testActivateModifyUserWithSuperAdminAndFailWithNoEmailAddress() throws Exception {
        superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountEntity.setUserFullName("full name");
        userAccountEntity.setEmailAddress(null);
        userAccountEntity.setActive(false);
        userAccountEntity = userAccountRepository.save(userAccountEntity);

        // add user to the super admin group
        Optional<SecurityGroupEntity> groupEntity
            = securityGroupRepository.findByGroupNameIgnoreCase(SecurityGroupEnum.SUPER_ADMIN.getName());
        Set<UserAccountEntity> entitiesSet = new HashSet<>();
        entitiesSet.add(userAccountEntity);

        groupEntity.get().setUsers(entitiesSet);
        dartsDatabase.addUserToGroup(userAccountEntity, groupEntity.get());

        HearingEntity hearingEntity
            = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME));

        dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity, userAccountEntity, TranscriptionStatusEnum.WITH_TRANSCRIBER);

        // now run the test to disable the user
        UserPatch userPatch = new UserPatch();
        userPatch.setDescription("test");
        userPatch.setActive(true);

        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT_URL + userAccountEntity.getId())
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(userPatch)))
            .andExpect(status().isConflict())
            .andReturn();

        uk.gov.hmcts.darts.transcriptions.model.Problem failureResponse = objectMapper
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(
            mvcResult.getResponse().getContentAsString(),
            uk.gov.hmcts.darts.transcriptions.model.Problem.class
        );

        // assert the failure response
        Assertions.assertEquals(UserManagementError.USER_ACTIVATION_FULLNAME_OR_EMAIL_VIOLATION.getType(), failureResponse.getType());

    }

    @Test
    void testActivateModifyUserWithSuperAdminAndFailWithNoFullNameAndNoEmailAddress() throws Exception {
        superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountEntity.setActive(false);
        userAccountEntity = userAccountRepository.save(userAccountEntity);

        // add user to the super admin group
        Optional<SecurityGroupEntity> groupEntity
            = securityGroupRepository.findByGroupNameIgnoreCase(SecurityGroupEnum.SUPER_ADMIN.getName());
        Set<UserAccountEntity> entitiesSet = new HashSet<>();
        entitiesSet.add(userAccountEntity);

        groupEntity.get().setUsers(entitiesSet);
        dartsDatabase.addUserToGroup(userAccountEntity, groupEntity.get());

        HearingEntity hearingEntity
            = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME));

        dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity, userAccountEntity, TranscriptionStatusEnum.WITH_TRANSCRIBER);

        // now run the test to disable the user
        UserPatch userPatch = new UserPatch();
        userPatch.setDescription("test");
        userPatch.setActive(true);

        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT_URL + userAccountEntity.getId())
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(userPatch)))
            .andExpect(status().isConflict())
            .andReturn();

        uk.gov.hmcts.darts.transcriptions.model.Problem failureResponse = objectMapper
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(
                mvcResult.getResponse().getContentAsString(),
                uk.gov.hmcts.darts.transcriptions.model.Problem.class
            );

        // assert the failure response
        Assertions.assertEquals(UserManagementError.USER_ACTIVATION_FULLNAME_OR_EMAIL_VIOLATION.getType(), failureResponse.getType());

    }

    @Test
    void testDeactivateFailureWhereUserIsLastInSuperAdminGroup() throws Exception {
        superAdminUserStub.givenSystemAdminIsAuthorised(userIdentity);

        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountEntity = userAccountRepository.save(userAccountEntity);

        // clear all users that are associated with the super admin group
        securityGroupStub.clearUsers(SecurityGroupEnum.SUPER_ADMIN);

        dartsDatabaseStub.addUserToGroup(userAccountEntity, SecurityGroupEnum.SUPER_ADMIN);

        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME));

        HearingEntity hearingEntity1 = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME));

        dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity, userAccountEntity, TranscriptionStatusEnum.WITH_TRANSCRIBER);
        dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity1, userAccountEntity, TranscriptionStatusEnum.WITH_TRANSCRIBER);

        UserPatch userPatch = new UserPatch();
        userPatch.setActive(false);

        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT_URL + userAccountEntity.getId())
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(userPatch)))
            .andExpect(status().is(AuthorisationError.UNABLE_TO_DEACTIVATE_USER.getHttpStatus().value()))
            .andReturn();

        objectMapper.registerModule(new JavaTimeModule());

        Problem problem = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                                                                           Problem.class);
        Assertions.assertEquals(AuthorisationError.UNABLE_TO_DEACTIVATE_USER.getErrorTypeNumeric(), problem.getType().toString());
    }

    @Test
    void testDeactivateFailureFromSuperUserModifyChange() throws Exception {
        superAdminUserStub.givenSystemUserIsAuthorised(userIdentity);

        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountEntity = userAccountRepository.save(userAccountEntity);

        dartsDatabaseStub.addUserToGroup(userAccountEntity, SecurityGroupEnum.SUPER_USER);

        UserPatch userPatch = new UserPatch();

        // force a failure on the call
        userPatch.setDescription("this should cause a failure");

        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT_URL + userAccountEntity.getId())
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(userPatch)))
            .andExpect(status().is(AuthorisationError.USER_NOT_AUTHORISED_TO_USE_PAYLOAD_CONTENT.getHttpStatus().value()))
            .andReturn();

        objectMapper.registerModule(new JavaTimeModule());

        Problem problem = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                                           Problem.class);
        Assertions.assertEquals(AuthorisationError.USER_NOT_AUTHORISED_TO_USE_PAYLOAD_CONTENT.getErrorTypeNumeric(), problem.getType().toString());
    }

    @Test
    void testActivateFailureFromSuperUser() throws Exception {
        superAdminUserStub.givenSystemUserIsAuthorised(userIdentity);

        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountEntity = userAccountRepository.save(userAccountEntity);

        dartsDatabaseStub.addUserToGroup(userAccountEntity, SecurityGroupEnum.SUPER_USER);

        UserPatch userPatch = new UserPatch();

        // force a failure on the call
        userPatch.setActive(true);

        MvcResult mvcResult = mockMvc.perform(patch(ENDPOINT_URL + userAccountEntity.getId())
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(userPatch)))
            .andExpect(status().is(AuthorisationError.USER_NOT_AUTHORISED_TO_ACTIVATE_USER.getHttpStatus().value()))
            .andReturn();

        objectMapper.registerModule(new JavaTimeModule());

        Problem problem = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                                           Problem.class);
        Assertions.assertEquals(AuthorisationError.USER_NOT_AUTHORISED_TO_ACTIVATE_USER.getErrorTypeNumeric(), problem.getType().toString());
    }

    private boolean containsApprovedWorkflow(List<TranscriptionWorkflowEntity> transcriptionWorkflowEntities) {
        return transcriptionWorkflowEntities.stream().anyMatch(e -> e.getTranscriptionStatus().getId()
            .equals(TranscriptionStatusEnum.APPROVED.getId()));
    }
}