package uk.gov.hmcts.darts.transcriptions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.data.UserAccountTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.TransactionDocumentStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionDocumentStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.AdminMarkedForDeletionResponseItem;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDetailAdminResponse;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionDocumentByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.Problem;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentRequest;
import uk.gov.hmcts.darts.transcriptions.model.SearchTranscriptionDocumentResponse;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TranscriptionControllerAdminGetTranscriptionIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/transcriptions?user_id=${USERID}&requested_at_from=${REQUESTED_FROM}";

    private static final String ENDPOINT_URL_NO_DATE = "/admin/transcriptions?user_id=${USERID}";

    private static final String ENDPOINT_URL_NO_USER = "/admin/transcriptions?requested_at_from=${REQUESTED_FROM}";

    private static final String ENDPOINT_DOCUMENT_SEARCH = "/admin/transcription-documents/search";

    private static final String ENDPOINT_GET_DOCUMENT_ID = "/admin/transcription-documents/";

    private static final String ENDPOINT_DOCUMENT_MARKED_FOR_DELETION = "/admin/transcription-documents/marked-for-deletion";


    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    private TranscriptionDocumentStub transcriptionDocumentStub;

    @Autowired
    private TranscriptionStub transcriptionStub;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TranscriptionStatusRepository transcriptionStatusRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransactionDocumentStub transactionDocumentStub;

    @Test
    void getTransactionsForUserWithoutDate() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        HearingEntity headerEntity = dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName(),
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        TranscriptionEntity transcriptionEntity = transcriptionStub.createTranscription(headerEntity);

        MvcResult mvcResult = mockMvc.perform(get(getTranscriptionsEndpointUrl(transcriptionEntity.getCreatedById().toString(), null)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        GetTranscriptionDetailAdminResponse[] transcriptionResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailAdminResponse[].class);

        assertEquals(1, transcriptionResponses.length);
        assertNotNull(transcriptionResponses[0].getHearingDate());
        assertEquals(transcriptionEntity.getId(), transcriptionResponses[0].getTranscriptionId());
        assertTrue(transcriptionResponses[0].getIsManualTranscription());
        assertEquals(headerEntity.getCourtCase().getCaseNumber(), transcriptionResponses[0].getCaseNumber());
        assertEquals(TranscriptionStatusEnum.APPROVED.getId(), transcriptionResponses[0].getTranscriptionStatusId());
        assertEquals(headerEntity.getCourtCase().getCourthouse().getId(),
                     transcriptionResponses[0].getCourthouseId());
        assertEquals(transcriptionEntity.getCreatedDateTime()
                         .atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), transcriptionResponses[0].getRequestedAt());
    }

    @Test
    void getTransactionsForUserWithoutDateAndWithoutHearing() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountEntity = userAccountRepository.save(userAccountEntity);

        TranscriptionEntity transcriptionEntity
            = transcriptionStub.createTranscription((HearingEntity) null, userAccountEntity);

        MvcResult mvcResult = mockMvc.perform(get(getTranscriptionsEndpointUrl(transcriptionEntity.getCreatedById().toString(),
                                                                               null)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        GetTranscriptionDetailAdminResponse[] transcriptionResponses = objectMapper
            .readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailAdminResponse[].class);

        assertEquals(1, transcriptionResponses.length);
        assertNull(transcriptionResponses[0].getHearingDate());
        assertEquals(transcriptionEntity.getId(), transcriptionResponses[0].getTranscriptionId());
        assertTrue(transcriptionResponses[0].getIsManualTranscription());
        assertNull(transcriptionResponses[0].getCaseNumber());
        assertEquals(TranscriptionStatusEnum.APPROVED.getId(), transcriptionResponses[0].getTranscriptionStatusId());
        assertNull(transcriptionResponses[0].getCourthouseId());
        assertEquals(transcriptionEntity.getCreatedDateTime()
                         .atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), transcriptionResponses[0].getRequestedAt());
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDate() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        HearingEntity headerEntity = dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName(),
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        TranscriptionEntity transcriptionEntity = transcriptionStub.createTranscription(headerEntity);

        MvcResult mvcResult = mockMvc.perform(get(getTranscriptionsEndpointUrl(transcriptionEntity.getCreatedById().toString(),
                                                                               OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        GetTranscriptionDetailAdminResponse[] transcriptionResponses = objectMapper
            .readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailAdminResponse[].class);

        assertEquals(1, transcriptionResponses.length);
        assertNotNull(transcriptionResponses[0].getHearingDate());
        assertEquals(transcriptionEntity.getId(), transcriptionResponses[0].getTranscriptionId());
        assertTrue(transcriptionResponses[0].getIsManualTranscription());
        assertEquals(headerEntity.getCourtCase().getCaseNumber(), transcriptionResponses[0].getCaseNumber());
        assertEquals(TranscriptionStatusEnum.APPROVED.getId(), transcriptionResponses[0].getTranscriptionStatusId());
        assertEquals(headerEntity.getCourtCase().getCourthouse().getId(), transcriptionResponses[0].getCourthouseId());
        assertEquals(transcriptionEntity.getCreatedDateTime()
                         .atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), transcriptionResponses[0].getRequestedAt());
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDateWithAssociatedWorkflow() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        HearingEntity headerEntity = dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName(),
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );


        TranscriptionStatusEntity statusEntity = transcriptionStub
            .getTranscriptionStatusByEnum(TranscriptionStatusEnum.WITH_TRANSCRIBER);

        TranscriptionEntity transcriptionEntity = transcriptionStub.createTranscription(headerEntity);
        UserAccountEntity createdBy = dartsDatabase.getUserAccountRepository().findById(transcriptionEntity.getCreatedById()).orElseThrow();
        TranscriptionEntity transcriptionEntity1
            = transcriptionStub.createTranscription(headerEntity, createdBy, TranscriptionStatusEnum.WITH_TRANSCRIBER);


        transcriptionStub.createTranscriptionWorkflowEntity(transcriptionEntity1,
                                                            courtroomAtNewcastleEntity.getCreatedById(),
                                                            OffsetDateTime.now(),
                                                            statusEntity);

        MvcResult mvcResult = mockMvc.perform(get(getTranscriptionsEndpointUrl(transcriptionEntity.getCreatedById().toString(),
                                                                               OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        GetTranscriptionDetailAdminResponse[] transcriptionResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailAdminResponse[].class);

        assertEquals(2, transcriptionResponses.length);
        assertNotNull(transcriptionResponses[0].getHearingDate());
        assertEquals(transcriptionEntity.getId(), transcriptionResponses[0].getTranscriptionId());
        assertTrue(transcriptionResponses[0].getIsManualTranscription());
        assertEquals(headerEntity.getCourtCase().getCaseNumber(), transcriptionResponses[0].getCaseNumber());
        assertEquals(TranscriptionStatusEnum.APPROVED.getId(), transcriptionResponses[0].getTranscriptionStatusId());
        assertEquals(headerEntity.getCourtCase().getCourthouse().getId(), transcriptionResponses[0].getCourthouseId());
        assertEquals(transcriptionEntity.getCreatedDateTime()
                         .atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), transcriptionResponses[0].getRequestedAt());

        assertNotNull(transcriptionResponses[1].getHearingDate());
        assertEquals(transcriptionEntity1.getId(), transcriptionResponses[1].getTranscriptionId());
        assertTrue(transcriptionResponses[1].getIsManualTranscription());
        assertEquals(headerEntity.getCourtCase().getCaseNumber(), transcriptionResponses[1].getCaseNumber());
        assertEquals(TranscriptionStatusEnum.WITH_TRANSCRIBER.getId(), transcriptionResponses[1].getTranscriptionStatusId());
        assertEquals(headerEntity.getCourtCase().getCourthouse().getId(), transcriptionResponses[1].getCourthouseId());
        assertEquals(transcriptionEntity1.getCreatedDateTime()
                         .atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), transcriptionResponses[1].getRequestedAt());
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDateUserNotExist() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        HearingEntity headerEntity = dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName(),
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        transcriptionStub.createTranscription(headerEntity);

        Integer userNotExistId = -500;
        MvcResult mvcResult = mockMvc.perform(get(getTranscriptionsEndpointUrl(userNotExistId.toString(),
                                                                               OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().isNotFound())
            .andReturn();

        Problem problem = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                                                 Problem.class);
        assertEquals(UserManagementError.USER_NOT_FOUND.getErrorTypeNumeric(), problem.getType().toString());
    }

    @Test
    void adminGetTranscriptions_shouldNotIncludedNonCurrentTranscriptions() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        HearingEntity headerEntity = dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName(),
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        TranscriptionEntity transcriptionEntity = transcriptionStub.createTranscription(headerEntity);
        transcriptionEntity.setIsCurrent(false);
        dartsDatabase.save(transcriptionEntity);

        MvcResult mvcResult = mockMvc.perform(get(getTranscriptionsEndpointUrl(transcriptionEntity.getCreatedById().toString(),
                                                                               OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        GetTranscriptionDetailAdminResponse[] transcriptionResponses = objectMapper
            .readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailAdminResponse[].class);

        assertEquals(0, transcriptionResponses.length);
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDateNoTransactions() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);
        UserAccountEntity userAccountEntity = UserAccountTestData.minimalUserAccount();
        userAccountRepository.save(userAccountEntity);

        MvcResult mvcResult = mockMvc.perform(get(getTranscriptionsEndpointUrl(userAccountEntity.getId().toString(),
                                                                               OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        GetTranscriptionDetailAdminResponse[] transcriptionResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailAdminResponse[].class);

        assertEquals(0, transcriptionResponses.length);
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDateCallerForbidden() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.DAR_PC);

        mockMvc.perform(get(getTranscriptionsEndpointUrl("2",
                                                         OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDateNoUserSpecified() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.DAR_PC);

        mockMvc.perform(get(getTranscriptionsEndpointUrl(null,
                                                         OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().isBadRequest())
            .andReturn();
    }


    @Test
    void documentSearch_superUser_shouldSeeNotHiddenDocuments() throws Exception {
        List<TranscriptionDocumentEntity> transcriptionDocumentResults = transcriptionDocumentStub.generateTranscriptionEntities(4, 1, false, true, false,
                                                                                                                                 true);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);
        when(userIdentity.userHasGlobalAccess(Set.of(SecurityRoleEnum.SUPER_ADMIN))).thenReturn(false);
        transcriptionDocumentResults.getFirst().setHidden(true);
        transcriptionDocumentResults.getLast().setHidden(true);
        dartsDatabase.save(transcriptionDocumentResults.getFirst());
        dartsDatabase.save(transcriptionDocumentResults.getLast());

        SearchTranscriptionDocumentRequest request = new SearchTranscriptionDocumentRequest();
        request.setCaseNumber(transcriptionDocumentResults.getFirst().getTranscription().getHearing().getCourtCase().getCaseNumber());

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_DOCUMENT_SEARCH)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        JSONAssert.assertEquals(
            """
                [
                   {
                     "transcription_document_id": 3,
                     "transcription_id": 3,
                     "courthouse": {
                       "display_name": "COURT NAME:1POST COURTHOUSE"
                     },
                     "hearing": {
                       "hearing_date": "%DATE_3%"
                     },
                     "is_manual_transcription": false,
                     "is_hidden": false,
                     "case": {
                       "id": 1,
                       "case_number": "%CASE_NUMBER%"
                     }
                   },
                   {
                     "transcription_document_id": 2,
                     "transcription_id": 2,
                     "courthouse": {
                       "display_name": "COURT NAME:1POST COURTHOUSE"
                     },
                     "hearing": {
                       "hearing_date": "%DATE_2%"
                     },
                     "is_manual_transcription": false,
                     "is_hidden": false,
                     "case": {
                       "id": 1,
                       "case_number": "%CASE_NUMBER%"
                     }
                   }
                 ]
                """
                .replace("%CASE_NUMBER%", request.getCaseNumber())
                .replace("%DATE_2%", OffsetDateTime.now().plusDays(4).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .replace("%DATE_3%", OffsetDateTime.now().plusDays(8).format(DateTimeFormatter.ISO_LOCAL_DATE)),
            mvcResult.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    void documentSearch_SuperAdmin_shouldSeeHiddenDocuments() throws Exception {
        List<TranscriptionDocumentEntity> transcriptionDocumentResults = transcriptionDocumentStub.generateTranscriptionEntities(4, 1, false, true, false,
                                                                                                                                 true);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);
        when(userIdentity.userHasGlobalAccess(Set.of(SecurityRoleEnum.SUPER_ADMIN))).thenReturn(true);
        transcriptionDocumentResults.getFirst().setHidden(true);
        transcriptionDocumentResults.getLast().setHidden(true);
        dartsDatabase.save(transcriptionDocumentResults.getFirst());
        dartsDatabase.save(transcriptionDocumentResults.getLast());

        SearchTranscriptionDocumentRequest request = new SearchTranscriptionDocumentRequest();
        request.setCaseNumber(transcriptionDocumentResults.getFirst().getTranscription().getHearing().getCourtCase().getCaseNumber());

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_DOCUMENT_SEARCH)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        JSONAssert.assertEquals(
            """
                [
                    {
                      "transcription_document_id": 4,
                      "transcription_id": 4,
                      "courthouse": {
                        "display_name": "COURT NAME:1POST COURTHOUSE"
                      },
                      "hearing": {
                        "hearing_date": "%DATE_4%"
                      },
                      "is_manual_transcription": false,
                      "is_hidden": true,
                      "case": {
                        "id": 1,
                        "case_number": "%CASE_NUMBER%"
                      }
                    },
                    {
                      "transcription_document_id": 3,
                      "transcription_id": 3,
                      "courthouse": {
                        "display_name": "COURT NAME:1POST COURTHOUSE"
                      },
                      "hearing": {
                        "hearing_date": "%DATE_3%"
                      },
                      "is_manual_transcription": false,
                      "is_hidden": false,
                      "case": {
                        "id": 1,
                        "case_number": "%CASE_NUMBER%"
                      }
                    },
                    {
                      "transcription_document_id": 2,
                      "transcription_id": 2,
                      "courthouse": {
                        "display_name": "COURT NAME:1POST COURTHOUSE"
                      },
                      "hearing": {
                        "hearing_date": "%DATE_2%"
                      },
                      "is_manual_transcription": false,
                      "is_hidden": false,
                      "case": {
                        "id": 1,
                        "case_number": "%CASE_NUMBER%"
                      }
                    },
                    {
                      "transcription_document_id": 1,
                      "transcription_id": 1,
                      "courthouse": {
                        "display_name": "COURT NAME:1POST COURTHOUSE"
                      },
                      "hearing": {
                        "hearing_date": "%DATE_1%"
                      },
                      "is_manual_transcription": false,
                      "is_hidden": true,
                      "case": {
                        "id": 1,
                        "case_number": "%CASE_NUMBER%"
                      }
                    }
                  ]
                """
                .replace("%CASE_NUMBER%", request.getCaseNumber())
                .replace("%DATE_1%", OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .replace("%DATE_2%", OffsetDateTime.now().plusDays(4).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .replace("%DATE_3%", OffsetDateTime.now().plusDays(8).format(DateTimeFormatter.ISO_LOCAL_DATE))
                .replace("%DATE_4%", OffsetDateTime.now().plusDays(12).format(DateTimeFormatter.ISO_LOCAL_DATE)),
            mvcResult.getResponse().getContentAsString(), JSONCompareMode.STRICT);
    }

    @Test
    void testSearchForTranscriptionDocumentWithCaseNumberAndReturnApplicableResults() throws Exception {
        List<TranscriptionDocumentEntity> transcriptionDocumentResults = transcriptionDocumentStub.generateTranscriptionEntities(4, 1, 1, false, true, false);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        SearchTranscriptionDocumentRequest request = new SearchTranscriptionDocumentRequest();
        request.setCaseNumber(transcriptionDocumentResults.get(2).getTranscription().getHearing().getCourtCase().getCaseNumber());

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_DOCUMENT_SEARCH)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTranscriptionDocumentResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTranscriptionDocumentResponse[].class);
        assertEquals(1, transformedMediaResponses.length);

        transactionalUtil.executeInTransaction(() -> {
            assertResponseEquality(transformedMediaResponses[0],
                                   getTranscriptionDocumentEntity(transformedMediaResponses[0].getTranscriptionDocumentId()));
        });
    }

    @Test
    void searchForTranscriptionDocument_shouldExcludeIsCurrentFalse() throws Exception {
        List<TranscriptionDocumentEntity> transcriptionDocumentResults =
            transcriptionDocumentStub.generateTranscriptionEntities(2, 1, false, true, false, true);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        TranscriptionDocumentEntity transcriptionDocumentEntity1 = transcriptionDocumentResults.get(0);
        TranscriptionEntity transcription1 = transcriptionDocumentEntity1.getTranscription();

        TranscriptionDocumentEntity transcriptionDocumentEntity2 = transcriptionDocumentResults.get(1);
        TranscriptionEntity transcription2 = transcriptionDocumentEntity2.getTranscription();
        transcription2.setIsCurrent(false);

        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcription2);


        SearchTranscriptionDocumentRequest request = new SearchTranscriptionDocumentRequest();
        request.setCaseNumber(transcription1.getHearing().getCourtCase().getCaseNumber());

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_DOCUMENT_SEARCH)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTranscriptionDocumentResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTranscriptionDocumentResponse[].class);
        assertEquals(1, transformedMediaResponses.length);

        transactionalUtil.executeInTransaction(() -> {
            assertResponseEquality(transformedMediaResponses[0],
                                   getTranscriptionDocumentEntity(transformedMediaResponses[0].getTranscriptionDocumentId()));
        });
    }


    @Test
    void testSearchForTranscriptionDocument_multipleResultsReturned_shouldBeOrderedByTranscriptId() throws Exception {
        transcriptionDocumentStub.generateTranscriptionEntities(4, 1, 1, false, true, false);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        SearchTranscriptionDocumentRequest request = new SearchTranscriptionDocumentRequest();

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_DOCUMENT_SEARCH)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTranscriptionDocumentResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTranscriptionDocumentResponse[].class);
        assertEquals(4, transformedMediaResponses.length);
        assertEquals(4, transformedMediaResponses[0].getTranscriptionDocumentId());
        assertEquals(3, transformedMediaResponses[1].getTranscriptionDocumentId());
        assertEquals(2, transformedMediaResponses[2].getTranscriptionDocumentId());
        assertEquals(1, transformedMediaResponses[3].getTranscriptionDocumentId());

    }

    @Test
    void testSearchForTranscriptionDocumentWithDateFromAndReturnApplicableResults() throws Exception {
        List<TranscriptionDocumentEntity> transcriptionDocumentResults = transcriptionDocumentStub
            .generateTranscriptionEntities(4, 1, 1, true, false, false);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        SearchTranscriptionDocumentRequest request = new SearchTranscriptionDocumentRequest();
        request.setRequestedAtFrom(transcriptionDocumentResults.get(2).getTranscription().getCreatedDateTime().minusDays(2).toLocalDate());

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_DOCUMENT_SEARCH)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTranscriptionDocumentResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTranscriptionDocumentResponse[].class);
        assertEquals(transcriptionDocumentResults.size(), transformedMediaResponses.length);

        transactionalUtil.executeInTransaction(() -> {
            for (SearchTranscriptionDocumentResponse response : transformedMediaResponses) {
                assertResponseEquality(response, getTranscriptionDocumentEntity(response.getTranscriptionDocumentId()));
            }
        });
    }

    @Test
    void testSearchForTranscriptionDocumentWithDateFromAndDateToAndReturnApplicableResults() throws Exception {
        List<TranscriptionDocumentEntity> transcriptionDocumentResults = transcriptionDocumentStub.generateTranscriptionEntities(4, 1, 1, true, false, false);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        SearchTranscriptionDocumentRequest request = new SearchTranscriptionDocumentRequest();
        request.setRequestedAtFrom(transcriptionDocumentResults.get(2).getTranscription().getCreatedDateTime().minusDays(2).toLocalDate());
        request.setRequestedAtTo(transcriptionDocumentResults.get(2).getTranscription().getCreatedDateTime().toLocalDate());

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_DOCUMENT_SEARCH)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTranscriptionDocumentResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTranscriptionDocumentResponse[].class);
        assertEquals(transcriptionDocumentResults.size(), transformedMediaResponses.length);

        transactionalUtil.executeInTransaction(() -> {
            for (SearchTranscriptionDocumentResponse response : transformedMediaResponses) {
                assertResponseEquality(response, getTranscriptionDocumentEntity(response.getTranscriptionDocumentId()));
            }
        });
    }

    @Test
    void testSearchForTranscriptionDocumentWithDateFromAndDateToAndReturnNoResults() throws Exception {
        List<TranscriptionDocumentEntity> transcriptionDocumentResults = transcriptionDocumentStub.generateTranscriptionEntities(4, 1, 1, true, false, false);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        SearchTranscriptionDocumentRequest request = new SearchTranscriptionDocumentRequest();
        request.setRequestedAtFrom(transcriptionDocumentResults.get(2).getTranscription().getCreatedDateTime().minusDays(2).toLocalDate());
        request.setRequestedAtTo(transcriptionDocumentResults.get(2).getTranscription().getCreatedDateTime().minusDays(1).toLocalDate());

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_DOCUMENT_SEARCH)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTranscriptionDocumentResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTranscriptionDocumentResponse[].class);
        assertEquals(0, transformedMediaResponses.length);
    }

    @Test
    void testSearchForTranscriptionDocumentUsingAllSearchCriteria() throws Exception {
        List<TranscriptionDocumentEntity> transcriptionDocumentResults = transcriptionDocumentStub.generateTranscriptionEntities(4, 1, 1, true, false, true);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        TranscriptionDocumentEntity mediaEntityToRequest = transcriptionDocumentResults.get(2);

        UserAccountEntity transcriptionCreatedBy = dartsDatabase.getUserAccountRepository()
            .findById(mediaEntityToRequest.getTranscription().getCreatedById()).orElseThrow();

        // use all search criteria
        SearchTranscriptionDocumentRequest request = new SearchTranscriptionDocumentRequest();
        request.setRequestedAtFrom(mediaEntityToRequest.getTranscription().getCreatedDateTime().minusDays(2).toLocalDate());
        request.setRequestedAtTo(mediaEntityToRequest.getTranscription().getCreatedDateTime().minusDays(1).toLocalDate());
        request.setCaseNumber(mediaEntityToRequest.getTranscription().getHearing().getCourtCase().getCaseNumber());
        request.setHearingDate(mediaEntityToRequest.getTranscription().getHearing().getHearingDate());
        request.setOwner(mediaEntityToRequest.getTranscription().getTranscriptionWorkflowEntities().getFirst().getWorkflowActor().getUserFullName());
        request.setRequestedBy(transcriptionCreatedBy.getUserFullName());
        request.setRequestedAtFrom(mediaEntityToRequest.getTranscription().getCreatedDateTime().toLocalDate());
        request.setRequestedAtTo(mediaEntityToRequest.getTranscription().getCreatedDateTime().toLocalDate());

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_DOCUMENT_SEARCH)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTranscriptionDocumentResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTranscriptionDocumentResponse[].class);
        assertEquals(1, transformedMediaResponses.length);
        transactionalUtil.executeInTransaction(() -> {
            assertResponseEquality(transformedMediaResponses[0],
                                   getTranscriptionDocumentEntity(transformedMediaResponses[0].getTranscriptionDocumentId()));
        });

    }

    @Test
    void testSearchForTranscriptionDocumentWithoutCriteriaAndReturnAll() throws Exception {
        List<TranscriptionDocumentEntity> transcriptionDocumentResults = transcriptionDocumentStub.generateTranscriptionEntities(4, 1, 1, true, false, true);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_DOCUMENT_SEARCH)
                                                  .header("Content-Type", "application/json")
                                                  .content("{}"))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTranscriptionDocumentResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTranscriptionDocumentResponse[].class);
        assertEquals(transcriptionDocumentResults.size(), transformedMediaResponses.length);

        transactionalUtil.executeInTransaction(() -> {
            for (SearchTranscriptionDocumentResponse response : transformedMediaResponses) {
                assertResponseEquality(response, getTranscriptionDocumentEntity(response.getTranscriptionDocumentId()));
            }
        });
    }


    @Test
    void transcriptionDocumentSearch_onHearingDate_shouldBringBackLegacyEntries() throws Exception {
        List<TranscriptionDocumentEntity> transcriptionDocumentResultsMod = transcriptionDocumentStub.generateTranscriptionEntities(2, 1, 1, true, false,
                                                                                                                                    false);
        List<TranscriptionDocumentEntity> transcriptionDocumentResultsLegacy = transcriptionDocumentStub.generateTranscriptionEntitiesLegacy(2, 1, true, false,
                                                                                                                                             false);


        LocalDate hearingDate = LocalDate.now();
        //Check that only one for the two documents created for both legacy and mod have the same hearing date.
        //Ensure legacy and mod entries are setup correctly
        assertThat(transcriptionDocumentResultsLegacy.getFirst().getTranscription().getHearings()).isNullOrEmpty();
        assertThat(transcriptionDocumentResultsLegacy.getFirst().getTranscription().getHearingDate()).isEqualTo(hearingDate);
        assertThat(transcriptionDocumentResultsLegacy.get(1).getTranscription().getHearings()).isNullOrEmpty();
        assertThat(transcriptionDocumentResultsLegacy.get(1).getTranscription().getHearingDate()).isNotEqualTo(hearingDate);


        assertThat(transcriptionDocumentResultsMod.getFirst().getTranscription().getHearing().getHearingDate()).isEqualTo(hearingDate);
        assertThat(transcriptionDocumentResultsMod.getFirst().getTranscription().getHearingDate()).isNull();
        assertThat(transcriptionDocumentResultsMod.get(1).getTranscription().getHearing().getHearingDate()).isNotEqualTo(hearingDate);
        assertThat(transcriptionDocumentResultsMod.get(1).getTranscription().getHearingDate()).isNull();

        superAdminUserStub.givenUserIsAuthorised(userIdentity);


        List<TranscriptionDocumentEntity> transcriptionDocumentResults = new ArrayList<>();
        transcriptionDocumentResults.add(transcriptionDocumentResultsLegacy.getFirst());
        transcriptionDocumentResults.add(transcriptionDocumentResultsMod.getFirst());

        SearchTranscriptionDocumentRequest request = new SearchTranscriptionDocumentRequest();
        request.setHearingDate(hearingDate);
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_DOCUMENT_SEARCH)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTranscriptionDocumentResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTranscriptionDocumentResponse[].class);
        assertEquals(transcriptionDocumentResults.size(), transformedMediaResponses.length);


        transactionalUtil.executeInTransaction(() -> {
            for (SearchTranscriptionDocumentResponse response : transformedMediaResponses) {
                assertResponseEquality(response, getTranscriptionDocumentEntity(response.getTranscriptionDocumentId()));
            }
        });
    }


    @Test
    void transcriptionDocumentSearch_resultsShouldBeUnique_evenIfBothLegacyAndModCriteriaMatch() throws Exception {
        List<TranscriptionDocumentEntity> transcriptionDocumentResults = transcriptionDocumentStub.generateTranscriptionEntities(1, 1, 1,
                                                                                                                                 true, false, false);

        TranscriptionDocumentEntity transcriptionDocumentEntity = transcriptionDocumentResults.getFirst();
        transcriptionStub.transcriptionLinkedCaseEntity(transcriptionDocumentEntity.getTranscription(),
                                                        transcriptionDocumentEntity.getTranscription().getCourtCase(),
                                                        transcriptionDocumentEntity.getTranscription().getCourtroom().getCourthouse().getCourthouseName(),
                                                        transcriptionDocumentEntity.getTranscription().getCourtCase().getCaseNumber());


        superAdminUserStub.givenUserIsAuthorised(userIdentity);


        SearchTranscriptionDocumentRequest request = new SearchTranscriptionDocumentRequest();
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_DOCUMENT_SEARCH)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTranscriptionDocumentResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTranscriptionDocumentResponse[].class);
        assertEquals(transcriptionDocumentResults.size(), transformedMediaResponses.length);


        transactionalUtil.executeInTransaction(() -> {
            for (SearchTranscriptionDocumentResponse response : transformedMediaResponses) {
                assertResponseEquality(response, getTranscriptionDocumentEntity(response.getTranscriptionDocumentId()));
            }
        });
    }

    @Test
    void testSearchForTranscriptionDocumentNoRequestPayloadReturnsABadRequest() throws Exception {
        mockMvc.perform(post(ENDPOINT_DOCUMENT_SEARCH)
                            .header("Content-Type", "application/json"))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    void testSearchForTranscriptionDocumentAuthorisationProblem() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.DAR_PC);

        mockMvc.perform(post(ENDPOINT_DOCUMENT_SEARCH).header("Content-Type", "application/json").header("Content-Type", "application/json")
                            .content("{}")).andExpect(status().isForbidden()).andReturn();
    }

    @Test
    void testSearchForTranscriptionDocumentByIdSuccess() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        HearingEntity headerEntity = dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName(),
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        String fileName = "file";
        String fileType = "fileType";
        Integer fileBytes = 299;
        boolean hidden = true;

        TranscriptionEntity transcriptionEntity = transcriptionStub.createTranscription(headerEntity);
        TranscriptionDocumentEntity transcriptionDocumentEntity = transactionDocumentStub
            .createTranscriptionDocument(fileName, fileBytes, fileType, hidden, transcriptionEntity);

        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_GET_DOCUMENT_ID + transcriptionDocumentEntity.getId()).header("Content-Type", "application/json"))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        GetTranscriptionDocumentByIdResponse transcriptionResponse
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDocumentByIdResponse.class);

        assertEquals(transcriptionEntity.getId(), transcriptionResponse.getTranscriptionId());
        assertEquals(transcriptionDocumentEntity.getId(), transcriptionResponse.getTranscriptionDocumentId());
        assertEquals(fileName, transcriptionResponse.getFileName());
        assertEquals(fileBytes, transcriptionResponse.getFileSizeBytes());
        assertEquals(fileType, transcriptionResponse.getFileType());
        assertEquals(hidden, transcriptionResponse.getIsHidden());
        assertEquals(transcriptionDocumentEntity.getUploadedBy().getId(), transcriptionResponse.getUploadedBy());
        assertEquals(transcriptionDocumentEntity.getUploadedDateTime()
                         .atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), transcriptionResponse.getUploadedAt());
    }

    @Test
    void testSearchForTranscriptionDocumentByIdNotFound() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_GET_DOCUMENT_ID + 10).header("Content-Type", "application/json"))
            .andExpect(status().isNotFound())
            .andReturn();

        Problem transcriptionProblemResponse
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), Problem.class);

        assertEquals(transcriptionProblemResponse.getType(), TranscriptionApiError.TRANSCRIPTION_DOCUMENT_ID_NOT_FOUND.getType());
    }

    @Test
    void testSearchForTranscriptionDocumentByIdAuthorisationProblem() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.DAR_PC);

        mockMvc.perform(get(ENDPOINT_GET_DOCUMENT_ID + 10).header("Content-Type", "application/json"))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void testGetTranscriptionDocumentMarkedForDeletionWithResults() throws Exception {
        // TODO: Resume when all test disablements are re-enabled. See https://tools.hmcts.net/jira/browse/DMP-3821
    }

    @Test
    void testGetTranscriptionDocumentMarkedForDeletionNoResults() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_DOCUMENT_MARKED_FOR_DELETION)
                                                  .header("Content-Type", "application/json"))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        AdminMarkedForDeletionResponseItem[] responses = objectMapper.readValue(
            mvcResult.getResponse().getContentAsString(),
            AdminMarkedForDeletionResponseItem[].class
        );
        assertEquals(0, responses.length);
        assertEquals(200, mvcResult.getResponse().getStatus());
    }

    @Test
    void testGetTranscriptionDocumentMarkedForDeletionNotSuperAdmin() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.SUPER_USER);

        mockMvc.perform(get(ENDPOINT_DOCUMENT_MARKED_FOR_DELETION)
                            .header("Content-Type", "application/json"))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void getTransactionsForUserBeyondOrEqualToDateWithAssociatedApprovedWorkflow() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);
        OffsetDateTime now = OffsetDateTime.of(2024, 1, 23, 10, 0, 0, 0, ZoneOffset.UTC);
        CourtroomEntity courtroomAtNewcastleEntity = dartsDatabase.createCourtroomUnlessExists("Newcastle", "room_a");
        HearingEntity headerEntity = dartsDatabase.createHearing(
            courtroomAtNewcastleEntity.getCourthouse().getCourthouseName(),
            courtroomAtNewcastleEntity.getName(),
            "c1",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        TranscriptionStatusEntity statusEntity = transcriptionStub
            .getTranscriptionStatusByEnum(TranscriptionStatusEnum.APPROVED);

        TranscriptionEntity transcriptionEntity = transcriptionStub.createTranscription(headerEntity);
        UserAccountEntity createdBy = dartsDatabase.getUserAccountRepository().findById(transcriptionEntity.getCreatedById()).orElseThrow();
        TranscriptionEntity transcriptionEntity1
            = transcriptionStub.createTranscription(headerEntity, createdBy, TranscriptionStatusEnum.APPROVED);


        TranscriptionWorkflowEntity transcriptionWorkflowEntity = transcriptionStub
            .createTranscriptionWorkflowEntity(transcriptionEntity1,
                                               courtroomAtNewcastleEntity.getCreatedById(),
                                               now,
                                               statusEntity);
        transcriptionEntity1.setTranscriptionWorkflowEntities(List.of(transcriptionWorkflowEntity));
        dartsDatabase.getTranscriptionRepository().saveAndFlush(transcriptionEntity1);

        MvcResult mvcResult = mockMvc.perform(get(getTranscriptionsEndpointUrl(transcriptionEntity.getCreatedById().toString(),
                                                                               OffsetDateTime.now().minusMonths(2).toString())))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        GetTranscriptionDetailAdminResponse[] transcriptionResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), GetTranscriptionDetailAdminResponse[].class);

        assertEquals(2, transcriptionResponses.length);
        assertNotNull(transcriptionResponses[1].getHearingDate());
        assertEquals(transcriptionEntity1.getId(), transcriptionResponses[1].getTranscriptionId());
        assertTrue(transcriptionResponses[1].getIsManualTranscription());
        assertEquals(headerEntity.getCourtCase().getCaseNumber(), transcriptionResponses[1].getCaseNumber());
        assertEquals(TranscriptionStatusEnum.APPROVED.getId(), transcriptionResponses[1].getTranscriptionStatusId());
        assertEquals(headerEntity.getCourtCase().getCourthouse().getId(), transcriptionResponses[1].getCourthouseId());
        assertEquals(transcriptionEntity1.getCreatedDateTime()
                         .atZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime(), transcriptionResponses[1].getRequestedAt());
        assertEquals(now, transcriptionResponses[1].getApprovedAt());

    }

    private TranscriptionDocumentEntity getTranscriptionDocumentEntity(Long id) {
        return dartsDatabase.getTranscriptionDocumentRepository().findById(id).orElseThrow();
    }

    private void assertResponseEquality(SearchTranscriptionDocumentResponse response, TranscriptionDocumentEntity entity) {
        assertEquals(response.getTranscriptionDocumentId(), entity.getId());
        assertEquals(response.getTranscriptionId(), entity.getTranscription().getId());

        if (response.getHearing() != null) {
            if (CollectionUtils.isEmpty(entity.getTranscription().getHearings())) {
                assertEquals(response.getHearing().getHearingDate(), entity.getTranscription().getHearingDate());
            } else {
                assertEquals(response.getHearing().getHearingDate(), entity.getTranscription().getHearing().getHearingDate());
            }
        }

        if (response.getCourthouse() != null) {
            assertEquals(response.getCourthouse().getDisplayName(),
                         entity.getTranscription().getCourtHouse().get().getDisplayName());
        }

        if (response.getCase() != null) {
            assertEquals(response.getCase().getId(),
                         entity.getTranscription().getCourtCase().getId());
            assertEquals(response.getCase().getCaseNumber(),
                         entity.getTranscription().getCourtCase().getCaseNumber());
        }

        assertEquals(response.getIsManualTranscription(),
                     entity.getTranscription().getIsManualTranscription());
        assertEquals(response.getIsHidden(),
                     entity.isHidden());
    }


    private String getTranscriptionsEndpointUrl(String userId, String dateAndTime) {
        if (dateAndTime != null && userId != null) {
            return ENDPOINT_URL.replace("${USERID}", userId).replace("${REQUESTED_FROM}", dateAndTime);
        } else if (dateAndTime != null && userId == null) {
            return ENDPOINT_URL_NO_USER.replace("${REQUESTED_FROM}", dateAndTime);
        } else if (dateAndTime == null && userId != null) {
            return ENDPOINT_URL_NO_DATE.replace("${USERID}", userId);
        }

        throw new UnsupportedOperationException("Endpoint URL configuration is not correct");
    }
}