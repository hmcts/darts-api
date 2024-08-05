package uk.gov.hmcts.darts.authorisation.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.SecurityRoleEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.SecurityRoleRepository;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourthouseStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthorisationControllerIntTest extends PostgresIntegrationBase {

    private static final URI ENDPOINT = URI.create("/userstate");
    private static final String EMAIL_ADDRESS = "test@test.com";

    @Autowired
    private UserAccountStub userAccountStub;

    @Autowired
    private CourthouseStub courthouseStub;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockSecurityContextWithEmailAddress(emailAddress = EMAIL_ADDRESS)
    void getUserStateShouldSucceedWhenUserHasNoAssignedGroupsAndIsActive() throws Exception {
        // Given
        createAndSaveUser(EMAIL_ADDRESS, true);

        // When
        MvcResult mvcResult = mockMvc.perform(
                get(ENDPOINT))
            .andExpect(status().isOk())
            .andReturn();

        // Then
        JSONAssert.assertEquals("""
                                    {
                                       "userId": 15000,
                                       "userName": "Test UserFullName",
                                       "roles": [],
                                       "isActive": true
                                    }""",
                                mvcResult.getResponse().getContentAsString(),
                                JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @Test
    @WithMockSecurityContextWithEmailAddress(emailAddress = EMAIL_ADDRESS)
    void getUserStateShouldSucceedWhenUserHasOneAssignedGroupWithGlobalAccess() throws Exception {
        // Given
        UserAccountEntity userAccount = createAndSaveUser(EMAIL_ADDRESS, true);

        SecurityGroupEntity group = createAndSaveSecurityGroup("Test", SecurityRoleEnum.JUDICIARY, true);
        userAccount.setSecurityGroupEntities(Collections.singleton(group));
        dartsDatabase.getUserAccountRepository().save(userAccount);

        // When
        MvcResult mvcResult = mockMvc.perform(
                get(ENDPOINT))
            .andExpect(status().isOk())
            .andReturn();

        // Then
        JSONAssert.assertEquals("""
                                    {
                                      "userId": 15000,
                                      "userName": "Test UserFullName",
                                      "roles": [
                                        {
                                          "roleId": 1,
                                          "roleName": "JUDICIARY",
                                          "globalAccess": true,
                                          "courthouseIds": [],
                                          "permissions": [
                                            "VIEW_MY_TRANSCRIPTIONS",
                                            "SEARCH_CASES",
                                            "VIEW_MY_AUDIOS",
                                            "READ_JUDGES_NOTES",
                                            "EXPORT_PROCESSED_PLAYBACK_AUDIO",
                                            "REQUEST_TRANSCRIPTION",
                                            "REQUEST_AUDIO",
                                            "VIEW_DARTS_INBOX",
                                            "READ_TRANSCRIBED_DOCUMENT",
                                            "LISTEN_TO_AUDIO_FOR_PLAYBACK",
                                            "RETENTION_ADMINISTRATION",
                                            "UPLOAD_JUDGES_NOTES"
                                          ]
                                        }
                                      ],
                                      "isActive": true
                                    }""",
                                mvcResult.getResponse().getContentAsString(),
                                JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @Test
    @WithMockSecurityContextWithEmailAddress(emailAddress = EMAIL_ADDRESS)
    void getUserStateShouldSucceedWhenUserHasMultipleAssignedGroupsWithMixedGlobalAccessWithSameRole() throws Exception {
        // Given
        UserAccountEntity userAccount = createAndSaveUser(EMAIL_ADDRESS, true);

        SecurityGroupEntity globalJudiciaryGroup = createAndSaveSecurityGroup("Global Judiciary", SecurityRoleEnum.JUDICIARY, true);
        SecurityGroupEntity nonGlobalJudiciaryGroup = createAndSaveSecurityGroup("Non Global Judiciary", SecurityRoleEnum.JUDICIARY, false);
        userAccount.setSecurityGroupEntities(Set.of(globalJudiciaryGroup, nonGlobalJudiciaryGroup));
        dartsDatabase.getUserAccountRepository().save(userAccount);

        // When
        MvcResult mvcResult = mockMvc.perform(
                get(ENDPOINT))
            .andExpect(status().isOk())
            .andReturn();

        // Then
        JSONAssert.assertEquals("""
                                    {
                                      "userId": 15000,
                                      "userName": "Test UserFullName",
                                      "roles": [
                                        {
                                          "roleId": 1,
                                          "roleName": "JUDICIARY",
                                          "globalAccess": true,
                                          "courthouseIds": [],
                                          "permissions": [
                                            "VIEW_MY_TRANSCRIPTIONS",
                                            "SEARCH_CASES",
                                            "VIEW_MY_AUDIOS",
                                            "READ_JUDGES_NOTES",
                                            "EXPORT_PROCESSED_PLAYBACK_AUDIO",
                                            "REQUEST_TRANSCRIPTION",
                                            "REQUEST_AUDIO",
                                            "VIEW_DARTS_INBOX",
                                            "READ_TRANSCRIBED_DOCUMENT",
                                            "LISTEN_TO_AUDIO_FOR_PLAYBACK",
                                            "RETENTION_ADMINISTRATION",
                                            "UPLOAD_JUDGES_NOTES"
                                          ]
                                        }
                                      ],
                                      "isActive": true
                                    }""",
                                mvcResult.getResponse().getContentAsString(),
                                JSONCompareMode.NON_EXTENSIBLE
        );
    }

    /**
     * Confirmation of expected response per DMP-3673.
     */
    @Test
    @WithMockSecurityContextWithEmailAddress(emailAddress = EMAIL_ADDRESS)
    void getUserStateShouldSucceedWhenUserHasMultipleAssignedGroupsWithCourthousesAndMixedGlobalAccessWithSameRole() throws Exception {
        // Given
        UserAccountEntity userAccount = createAndSaveUser(EMAIL_ADDRESS, true);

        SecurityGroupEntity globalJudiciaryGroup = createAndSaveSecurityGroup("Global Judiciary", SecurityRoleEnum.JUDICIARY, true);
        SecurityGroupEntity nonGlobalJudiciaryGroup = createAndSaveSecurityGroup("Non Global Judiciary", SecurityRoleEnum.JUDICIARY, false);

        CourthouseEntity courthouse = courthouseStub.createMinimalCourthouse();
        nonGlobalJudiciaryGroup.setCourthouseEntities(Collections.singleton(courthouse));
        dartsDatabase.getSecurityGroupRepository().save(nonGlobalJudiciaryGroup);

        userAccount.setSecurityGroupEntities(Set.of(globalJudiciaryGroup, nonGlobalJudiciaryGroup));
        dartsDatabase.getUserAccountRepository().save(userAccount);

        // When
        MvcResult mvcResult = mockMvc.perform(
                get(ENDPOINT))
            .andExpect(status().isOk())
            .andReturn();

        // Then
        JSONAssert.assertEquals("""
                                    {
                                      "userId": 15000,
                                      "userName": "Test UserFullName",
                                      "roles": [
                                        {
                                          "roleId": 1,
                                          "roleName": "JUDICIARY",
                                          "globalAccess": true,
                                          "courthouseIds": [
                                            1
                                          ],
                                          "permissions": [
                                            "VIEW_MY_TRANSCRIPTIONS",
                                            "SEARCH_CASES",
                                            "VIEW_MY_AUDIOS",
                                            "READ_JUDGES_NOTES",
                                            "EXPORT_PROCESSED_PLAYBACK_AUDIO",
                                            "REQUEST_AUDIO",
                                            "REQUEST_TRANSCRIPTION",
                                            "VIEW_DARTS_INBOX",
                                            "READ_TRANSCRIBED_DOCUMENT",
                                            "LISTEN_TO_AUDIO_FOR_PLAYBACK",
                                            "RETENTION_ADMINISTRATION",
                                            "UPLOAD_JUDGES_NOTES"
                                          ]
                                        }
                                      ],
                                      "isActive": true
                                    }""",
                                mvcResult.getResponse().getContentAsString(),
                                JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @Test
    @WithMockSecurityContextWithEmailAddress(emailAddress = EMAIL_ADDRESS)
    void getUserStateShouldSucceedWhenUserHasMultipleAssignedGroupsAcrossMultipleRoles() throws Exception {
        // Given
        UserAccountEntity userAccount = createAndSaveUser(EMAIL_ADDRESS, true);

        SecurityGroupEntity judiciaryGroup = createAndSaveSecurityGroup("Global Judiciary", SecurityRoleEnum.JUDICIARY, true);
        SecurityGroupEntity approverGroup = createAndSaveSecurityGroup("Non Global Approver", SecurityRoleEnum.APPROVER, false);

        userAccount.setSecurityGroupEntities(Set.of(judiciaryGroup, approverGroup));
        dartsDatabase.getUserAccountRepository().save(userAccount);

        // When
        MvcResult mvcResult = mockMvc.perform(
                get(ENDPOINT))
            .andExpect(status().isOk())
            .andReturn();

        // Then
        JSONAssert.assertEquals("""
                                    {
                                      "userId": 15000,
                                      "userName": "Test UserFullName",
                                      "roles": [
                                        {
                                          "roleId": 3,
                                          "roleName": "APPROVER",
                                          "globalAccess": false,
                                          "courthouseIds": [],
                                          "permissions": [
                                            "VIEW_MY_TRANSCRIPTIONS",
                                            "APPROVE_REJECT_TRANSCRIPTION_REQUEST",
                                            "SEARCH_CASES",
                                            "VIEW_MY_AUDIOS",
                                            "EXPORT_PROCESSED_PLAYBACK_AUDIO",
                                            "REQUEST_TRANSCRIPTION",
                                            "REQUEST_AUDIO",
                                            "VIEW_DARTS_INBOX",
                                            "READ_TRANSCRIBED_DOCUMENT",
                                            "LISTEN_TO_AUDIO_FOR_PLAYBACK",
                                            "RETENTION_ADMINISTRATION"
                                          ]
                                        },
                                        {
                                          "roleId": 1,
                                          "roleName": "JUDICIARY",
                                          "globalAccess": true,
                                          "courthouseIds": [],
                                          "permissions": [
                                            "VIEW_MY_TRANSCRIPTIONS",
                                            "SEARCH_CASES",
                                            "VIEW_MY_AUDIOS",
                                            "READ_JUDGES_NOTES",
                                            "EXPORT_PROCESSED_PLAYBACK_AUDIO",
                                            "REQUEST_TRANSCRIPTION",
                                            "REQUEST_AUDIO",
                                            "VIEW_DARTS_INBOX",
                                            "READ_TRANSCRIBED_DOCUMENT",
                                            "LISTEN_TO_AUDIO_FOR_PLAYBACK",
                                            "UPLOAD_JUDGES_NOTES",
                                            "RETENTION_ADMINISTRATION"
                                          ]
                                        }
                                      ],
                                      "isActive": true
                                    }
                                    """,
                                mvcResult.getResponse().getContentAsString(),
                                JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @Test
    @WithMockSecurityContextWithEmailAddress(emailAddress = EMAIL_ADDRESS)
    void getUserStateShouldSucceedWhenUserHasADiverseSetOfAssignedGroups() throws Exception {
        // Given
        final UserAccountEntity userAccount = createAndSaveUser(EMAIL_ADDRESS, true);

        final SecurityGroupEntity globalJudiciaryGroup = createAndSaveSecurityGroup("Global Judiciary", SecurityRoleEnum.JUDICIARY, true);
        final SecurityGroupEntity nonGlobalJudiciaryGroup = createAndSaveSecurityGroup("Non Global Judiciary", SecurityRoleEnum.JUDICIARY, false);
        final SecurityGroupEntity approverGroup1 = createAndSaveSecurityGroup("Approver 1", SecurityRoleEnum.APPROVER, false);
        final SecurityGroupEntity approverGroup2 = createAndSaveSecurityGroup("Approver 2", SecurityRoleEnum.APPROVER, false);
        final SecurityGroupEntity requesterGroup = createAndSaveSecurityGroup("Requester", SecurityRoleEnum.REQUESTER, false);
        final SecurityGroupEntity translationGroup = createAndSaveSecurityGroup("Translation QA", SecurityRoleEnum.TRANSLATION_QA, false);

        final CourthouseEntity courthouse1 = courthouseStub.createCourthouseUnlessExists("Courthouse 1");
        final CourthouseEntity courthouse2 = courthouseStub.createCourthouseUnlessExists("Courthouse 2");
        final CourthouseEntity courthouse3 = courthouseStub.createCourthouseUnlessExists("Courthouse 3");

        nonGlobalJudiciaryGroup.setCourthouseEntities(Set.of(courthouse1, courthouse2, courthouse3));
        approverGroup1.setCourthouseEntities(Collections.singleton(courthouse1));
        approverGroup2.setCourthouseEntities(Collections.singleton(courthouse2));
        requesterGroup.setCourthouseEntities(Collections.singleton(courthouse1));
        translationGroup.setCourthouseEntities(Set.of(courthouse1, courthouse2));

        dartsDatabase.getSecurityGroupRepository().saveAll(Set.of(nonGlobalJudiciaryGroup,
                                                                  approverGroup1,
                                                                  approverGroup2,
                                                                  requesterGroup,
                                                                  translationGroup));

        userAccount.setSecurityGroupEntities(Set.of(globalJudiciaryGroup,
                                                    nonGlobalJudiciaryGroup,
                                                    approverGroup1,
                                                    approverGroup2,
                                                    requesterGroup,
                                                    translationGroup));
        dartsDatabase.getUserAccountRepository().save(userAccount);

        // When
        MvcResult mvcResult = mockMvc.perform(
                get(ENDPOINT))
            .andExpect(status().isOk())
            .andReturn();

        // Then
        JSONAssert.assertEquals("""
                                    {
                                      "userId": 15000,
                                      "userName": "Test UserFullName",
                                      "roles": [
                                        {
                                          "roleId": 2,
                                          "roleName": "REQUESTER",
                                          "globalAccess": false,
                                          "courthouseIds": [
                                            1
                                          ],
                                          "permissions": [
                                            "VIEW_MY_TRANSCRIPTIONS",
                                            "SEARCH_CASES",
                                            "VIEW_MY_AUDIOS",
                                            "EXPORT_PROCESSED_PLAYBACK_AUDIO",
                                            "REQUEST_TRANSCRIPTION",
                                            "REQUEST_AUDIO",
                                            "VIEW_DARTS_INBOX",
                                            "READ_TRANSCRIBED_DOCUMENT",
                                            "LISTEN_TO_AUDIO_FOR_PLAYBACK",
                                            "RETENTION_ADMINISTRATION"
                                          ]
                                        },
                                        {
                                          "roleId": 5,
                                          "roleName": "TRANSLATION_QA",
                                          "globalAccess": false,
                                          "courthouseIds": [
                                            1,
                                            2
                                          ],
                                          "permissions": [
                                            "SEARCH_CASES",
                                            "VIEW_MY_AUDIOS",
                                            "EXPORT_PROCESSED_PLAYBACK_AUDIO",
                                            "REQUEST_AUDIO",
                                            "VIEW_DARTS_INBOX",
                                            "READ_TRANSCRIBED_DOCUMENT",
                                            "LISTEN_TO_AUDIO_FOR_PLAYBACK"
                                          ]
                                        },
                                        {
                                          "roleId": 3,
                                          "roleName": "APPROVER",
                                          "globalAccess": false,
                                          "courthouseIds": [
                                            1,
                                            2
                                          ],
                                          "permissions": [
                                            "VIEW_MY_TRANSCRIPTIONS",
                                            "SEARCH_CASES",
                                            "APPROVE_REJECT_TRANSCRIPTION_REQUEST",
                                            "VIEW_MY_AUDIOS",
                                            "EXPORT_PROCESSED_PLAYBACK_AUDIO",
                                            "REQUEST_TRANSCRIPTION",
                                            "REQUEST_AUDIO",
                                            "VIEW_DARTS_INBOX",
                                            "READ_TRANSCRIBED_DOCUMENT",
                                            "LISTEN_TO_AUDIO_FOR_PLAYBACK",
                                            "RETENTION_ADMINISTRATION"
                                          ]
                                        },
                                        {
                                          "roleId": 1,
                                          "roleName": "JUDICIARY",
                                          "globalAccess": true,
                                          "courthouseIds": [
                                            1,
                                            2,
                                            3
                                          ],
                                          "permissions": [
                                            "VIEW_MY_TRANSCRIPTIONS",
                                            "SEARCH_CASES",
                                            "VIEW_MY_AUDIOS",
                                            "READ_JUDGES_NOTES",
                                            "EXPORT_PROCESSED_PLAYBACK_AUDIO",
                                            "REQUEST_AUDIO",
                                            "REQUEST_TRANSCRIPTION",
                                            "VIEW_DARTS_INBOX",
                                            "READ_TRANSCRIBED_DOCUMENT",
                                            "LISTEN_TO_AUDIO_FOR_PLAYBACK",
                                            "UPLOAD_JUDGES_NOTES",
                                            "RETENTION_ADMINISTRATION"
                                          ]
                                        }
                                      ],
                                      "isActive": true
                                    }""",
                                mvcResult.getResponse().getContentAsString(),
                                JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @Test
    @WithMockSecurityContextWithEmailAddress(emailAddress = EMAIL_ADDRESS)
    void getUserStateShouldFailWhenUserIsInactive() throws Exception {
        // Given
        createAndSaveUser(EMAIL_ADDRESS, false);

        // When
        MvcResult mvcResult = mockMvc.perform(
                get(ENDPOINT))
            .andExpect(status().isForbidden())
            .andReturn();

        // Then
        JSONAssert.assertEquals("""
                                    {
                                      "type": "AUTHORISATION_106",
                                      "title": "Could not obtain user details",
                                      "status": 403
                                    }""",
                                mvcResult.getResponse().getContentAsString(),
                                JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @Test
    @WithMockSecurityContextWithEmailAddress(emailAddress = "unknown@test.com")
    void getUserStateShouldFailWhenUserEmailDoesNotExistInDatabase() throws Exception {
        // Given
        createAndSaveUser(EMAIL_ADDRESS, true);

        // When
        MvcResult mvcResult = mockMvc.perform(
                get(ENDPOINT))
            .andExpect(status().isForbidden())
            .andReturn();

        // Then
        JSONAssert.assertEquals("""
                                    {
                                      "type": "AUTHORISATION_106",
                                      "title": "Could not obtain user details",
                                      "status": 403
                                    }""",
                                mvcResult.getResponse().getContentAsString(),
                                JSONCompareMode.NON_EXTENSIBLE
        );
    }

    private UserAccountEntity createAndSaveUser(String emailAddress, boolean isActive) {
        return userAccountStub.createIntegrationUser(null, "Test User", emailAddress, isActive);
    }

    private SecurityGroupEntity createAndSaveSecurityGroup(String name, SecurityRoleEnum securityRoleEnum, boolean isGlobalAccess) {
        SecurityGroupEntity groupEntity = new SecurityGroupEntity();
        groupEntity.setGroupName(name);
        groupEntity.setGlobalAccess(isGlobalAccess);
        groupEntity.setDisplayState(true);
        groupEntity.setUseInterpreter(false);

        SecurityRoleRepository roleRepository = dartsDatabase.getSecurityRoleRepository();
        SecurityRoleEntity roleEntity = roleRepository.findByRoleName(securityRoleEnum.name())
            .orElseThrow();
        roleRepository.save(roleEntity);

        groupEntity.setSecurityRoleEntity(roleEntity);

        return dartsDatabase.getSecurityGroupRepository()
            .save(groupEntity);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @WithSecurityContext(factory = MockSecurityContextWithEmailAddressFactory.class)
    public @interface WithMockSecurityContextWithEmailAddress {
        String emailAddress() default "";
    }

    public static class MockSecurityContextWithEmailAddressFactory implements WithSecurityContextFactory<WithMockSecurityContextWithEmailAddress> {
        @Override
        public SecurityContext createSecurityContext(WithMockSecurityContextWithEmailAddress annotation) {
            SecurityContext customContext = SecurityContextHolder.createEmptyContext();

            Instant now = Instant.now();
            Jwt jwt = new Jwt("dummy",
                              now,
                              now.plus(1, ChronoUnit.HOURS),
                              Map.of("typ", "jwt"),
                              Map.of("emails", annotation.emailAddress()
                              )
            );

            JwtAuthenticationToken jwtAuthenticationToken = new JwtAuthenticationToken(jwt);

            customContext.setAuthentication(jwtAuthenticationToken);

            return customContext;
        }
    }

}