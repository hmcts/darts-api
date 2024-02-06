package uk.gov.hmcts.darts.cases.controller;


import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.cases.model.Annotation;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.AnnotationStub;

import java.time.OffsetDateTime;
import java.util.List;

import static java.util.Comparator.comparing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@AutoConfigureMockMvc
class CaseControllerGetAnnotationsIntTest extends IntegrationBase {
    private static final OffsetDateTime CREATED_DATE = OffsetDateTime.parse("2023-07-31T12:00Z");

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-09-01T12:00Z");

    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";

    @Autowired
    AdminUserStub adminUserStub;

    @MockBean
    private UserIdentity mockUserIdentity;

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    AnnotationStub annotationStub;

    @Test
    void givenJudgeUserReturnOwnAnnotationsButNotDeleted() throws Exception {
        final CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "case1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub()
            .createHearing("Bristol", "1", "case1", CREATED_DATE.toLocalDate());

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        UserAccountEntity adminUser = dartsDatabase.getUserAccountStub()
            .createAdminUser();

        createAnnotation(adminUser, hearingEntity);
        final AnnotationEntity annotationEntity2 = createAnnotation(testUser, hearingEntity);
        final AnnotationEntity annotationEntity3 = createAnnotation(testUser, hearingEntity);
        createAnnotation(testUser, hearingEntity, true);

        MvcResult mvcResult = mockMvc.perform(get("/cases/" + courtCaseEntity.getId() + "/annotations")
                                                  .header("user_id", testUser.getId()))
            .andExpect(status().isOk())
            .andReturn();

        List<Annotation> annotations = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                                                              new TypeReference<List<Annotation>>(){});

        annotations.sort(comparing(Annotation::getAnnotationId));
        assertEquals(2, annotations.size());
        checkAnnotation(annotations.get(0), hearingEntity, testUser, annotationEntity2);
        checkAnnotation(annotations.get(1), hearingEntity, testUser, annotationEntity3);
    }

    @Test
    void givenJudgeMultipleHearingsInCaseGetAllAnnotations() throws Exception {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "case1");
        HearingEntity hearingEntity1 = dartsDatabase.getHearingStub()
            .createHearing("Bristol", "1", "case1", CREATED_DATE.toLocalDate());

        HearingEntity hearingEntity2 = dartsDatabase.getHearingStub()
            .createHearing("Bristol", "1", "case1",
                           CREATED_DATE.toLocalDate().plusMonths(1));

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        final AnnotationEntity annotationEntity1 = createAnnotation(testUser, hearingEntity1);
        final AnnotationEntity annotationEntity2 = createAnnotation(testUser, hearingEntity1);
        final AnnotationEntity annotationEntity3 = createAnnotation(testUser, hearingEntity2);
        final AnnotationEntity annotationEntity4 = createAnnotation(testUser, hearingEntity2);

        MvcResult mvcResult = mockMvc.perform(get("/cases/" + courtCaseEntity.getId() + "/annotations")
                                                  .header("user_id", testUser.getId()))
            .andExpect(status().isOk())
            .andReturn();

        List<Annotation> annotations = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                                                              new TypeReference<List<Annotation>>(){});

        annotations.sort(comparing(Annotation::getAnnotationId));
        assertEquals(4, annotations.size());
        checkAnnotation(annotations.get(0), hearingEntity1, testUser, annotationEntity1);
        checkAnnotation(annotations.get(1), hearingEntity1, testUser, annotationEntity2);
        checkAnnotation(annotations.get(2), hearingEntity2, testUser, annotationEntity3);
        checkAnnotation(annotations.get(3), hearingEntity2, testUser, annotationEntity4);
    }



    @Test
    void givenAdminUserReturnAllAnnotations() throws Exception {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "case1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub()
            .createHearing("Bristol", "1", "case1", CREATED_DATE.toLocalDate());

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAdminUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        UserAccountEntity adminUser = dartsDatabase.getUserAccountStub()
            .createAdminUser();
        UserAccountEntity judgeUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser("1");

        final AnnotationEntity annotationEntity1 = createAnnotation(adminUser, hearingEntity);
        final AnnotationEntity annotationEntity2 = createAnnotation(testUser, hearingEntity);
        final AnnotationEntity annotationEntity3 = createAnnotation(judgeUser, hearingEntity);

        MvcResult mvcResult = mockMvc.perform(get("/cases/" + courtCaseEntity.getId() + "/annotations")
                                                  .header("user_id", testUser.getId()))
            .andExpect(status().isOk())
            .andReturn();

        List<Annotation> annotations = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                                                              new TypeReference<List<Annotation>>(){});

        annotations.sort(comparing(Annotation::getAnnotationId));
        assertEquals(3, annotations.size());
        checkAnnotation(annotations.get(0), hearingEntity, adminUser, annotationEntity1);
        checkAnnotation(annotations.get(1), hearingEntity, testUser, annotationEntity2);
        checkAnnotation(annotations.get(2), hearingEntity, judgeUser, annotationEntity3);
    }

    @Test
    void givenNonAdminJudgeUserReturnForbidden() throws Exception {
        final CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "case1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub()
            .createHearing("Bristol", "1", "case1", CREATED_DATE.toLocalDate());

        UserAccountEntity judgeUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser("1");
        log.debug("judgeUser.getId() " + judgeUser.getId());

        createAnnotation(judgeUser, hearingEntity);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createTranscriptionCompanyUser(hearingEntity.getCourtroom().getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        mockMvc.perform(get("/cases/" + courtCaseEntity.getId() + "/annotations")
                            .header("user_id", testUser.getId()))
            .andExpect(status().isForbidden())
            .andReturn();

    }

    @Test
    void givenUserRequestsNonExistingHearingThenReturn404() throws Exception {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAdminUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        mockMvc.perform(get("/cases/200/annotations")
                            .header("user_id", testUser.getId()))
            .andExpect(status().isNotFound());
    }


    private void checkAnnotation(Annotation annotation, HearingEntity hearingEntity, UserAccountEntity user,
                                 AnnotationEntity annotationEntity) {
        assertEquals(annotationEntity.getId(), annotation.getAnnotationId());
        assertEquals("some text", annotation.getAnnotationText());
        assertEquals(hearingEntity.getId(), annotation.getHearingId());
        assertEquals(1, annotation.getAnnotationDocuments().size());
        assertEquals("a filename", annotation.getAnnotationDocuments().get(0).getFileName());
        assertEquals("DOC", annotation.getAnnotationDocuments().get(0).getFileType());
        assertEquals(user.getUserFullName(), annotation.getAnnotationDocuments().get(0).getUploadedBy());
    }

    private AnnotationEntity createAnnotation(UserAccountEntity user, HearingEntity hearingEntity, boolean deleted) {
        AnnotationEntity annotationEntity = annotationStub.createAndSaveAnnotationEntityWith(user, "some text");
        annotationEntity.addHearing(hearingEntity);
        annotationEntity.setDeleted(deleted);
        dartsDatabase.save(annotationEntity);

        annotationStub.createAndSaveAnnotationDocumentEntityWith(annotationEntity, "a filename", "DOC",
                                                                 900, user, OffsetDateTime.now(), "achecksum");
        return annotationEntity;
    }

    private AnnotationEntity createAnnotation(UserAccountEntity user, HearingEntity hearingEntity) {
        return createAnnotation(user, hearingEntity, false);
    }

}
