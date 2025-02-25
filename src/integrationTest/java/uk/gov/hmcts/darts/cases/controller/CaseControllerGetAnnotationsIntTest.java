package uk.gov.hmcts.darts.cases.controller;


import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.cases.model.Annotation;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AnnotationStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.DARTS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@Slf4j
@AutoConfigureMockMvc
class CaseControllerGetAnnotationsIntTest extends IntegrationBase {
    private static final OffsetDateTime CREATED_DATE = OffsetDateTime.parse("2023-07-31T12:00Z");

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    private UserIdentity mockUserIdentity;

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    private AnnotationStub annotationStub;

    @Test
    void givenJudgeUserReturnOwnAnnotationsButNotDeleted() throws Exception {
        final CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "case1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub()
            .createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(CREATED_DATE));

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        UserAccountEntity adminUser = dartsDatabase.getUserAccountStub()
            .createSuperAdminUser();

        createAnnotation(adminUser, hearingEntity);
        final AnnotationEntity annotationEntity2 = createAnnotation(testUser, hearingEntity);
        final AnnotationEntity annotationEntity3 = createAnnotation(testUser, hearingEntity);
        createAnnotation(testUser, hearingEntity, true);

        MvcResult mvcResult = mockMvc.perform(get("/cases/" + courtCaseEntity.getId() + "/annotations")
                                                  .header("user_id", testUser.getId()))
            .andExpect(status().isOk())
            .andReturn();

        List<Annotation> annotations = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                                                              new TypeReference<>() {
                                                              });

        assertEquals(2, annotations.size());
        checkAnnotation(annotations.get(0), hearingEntity, testUser, annotationEntity2);
        checkAnnotation(annotations.get(1), hearingEntity, testUser, annotationEntity3);
    }

    @Test
    void givenJudgeMultipleHearingsInCaseGetAllAnnotations() throws Exception {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "case1");
        HearingEntity hearingEntity1 = dartsDatabase.getHearingStub()
            .createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(CREATED_DATE));

        HearingEntity hearingEntity2 = dartsDatabase.getHearingStub()
            .createHearing("Bristol", "1", "case1",
                           DateConverterUtil.toLocalDateTime(CREATED_DATE).plusMonths(1));

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
                                                              new TypeReference<>() {
                                                              });

        assertEquals(4, annotations.size());
        checkAnnotation(annotations.get(0), hearingEntity2, testUser, annotationEntity3);
        checkAnnotation(annotations.get(1), hearingEntity2, testUser, annotationEntity4);
        checkAnnotation(annotations.get(2), hearingEntity1, testUser, annotationEntity1);
        checkAnnotation(annotations.get(3), hearingEntity1, testUser, annotationEntity2);
    }


    @Test
    void givenSuperAdminUserReturnAllAnnotations() throws Exception {
        CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "case1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub()
            .createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(CREATED_DATE));

        UserAccountEntity testUser = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        UserAccountEntity superAdminUser = dartsDatabase.getUserAccountStub()
            .createSuperAdminUser();
        UserAccountEntity judgeUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser("1");

        final AnnotationEntity annotationEntity1 = createAnnotation(superAdminUser, hearingEntity);
        final AnnotationEntity annotationEntity2 = createAnnotation(testUser, hearingEntity);
        final AnnotationEntity annotationEntity3 = createAnnotation(judgeUser, hearingEntity);

        MvcResult mvcResult = mockMvc.perform(get("/cases/" + courtCaseEntity.getId() + "/annotations")
                                                  .header("user_id", testUser.getId()))
            .andExpect(status().isOk())
            .andReturn();

        List<Annotation> annotations = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                                                              new TypeReference<>() {
                                                              });

        assertEquals(3, annotations.size());
        checkAnnotation(annotations.get(0), hearingEntity, superAdminUser, annotationEntity1);
        checkAnnotation(annotations.get(1), hearingEntity, testUser, annotationEntity2);
        checkAnnotation(annotations.get(2), hearingEntity, judgeUser, annotationEntity3);
    }

    @Test
    void givenNonSuperAdminJudgeUserReturnForbidden() throws Exception {
        final CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "case1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub()
            .createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(CREATED_DATE));

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

        verify(mockUserIdentity).getUserAccount();
        verify(mockUserIdentity).userHasGlobalAccess(Set.of(JUDICIARY, SUPER_ADMIN, DARTS));
        verify(mockUserIdentity, atLeastOnce()).getUserIdFromJwt();//Called by AuditorRevisionListener
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void givenSuperUserReturnForbidden() throws Exception {
        final CourtCaseEntity courtCaseEntity = dartsDatabase.createCase("Bristol", "case1");
        HearingEntity hearingEntity = dartsDatabase.getHearingStub()
            .createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(CREATED_DATE));

        UserAccountEntity judgeUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser("1");
        log.debug("judgeUser.getId() " + judgeUser.getId());

        createAnnotation(judgeUser, hearingEntity);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createSuperUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        mockMvc.perform(get("/cases/" + courtCaseEntity.getId() + "/annotations")
                            .header("user_id", testUser.getId()))
            .andExpect(status().isForbidden())
            .andReturn();

        verify(mockUserIdentity).getUserAccount();
        verify(mockUserIdentity).userHasGlobalAccess(Set.of(JUDICIARY, SUPER_ADMIN, DARTS));
        verify(mockUserIdentity, atLeastOnce()).getUserIdFromJwt();//Called by AuditorRevisionListener
        verifyNoMoreInteractions(mockUserIdentity);
    }

    @Test
    void givenUserRequestsNonExistingHearingThenReturn404() throws Exception {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createSuperAdminUser();
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
