package uk.gov.hmcts.darts.hearings.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Slf4j
@AutoConfigureMockMvc
class HearingsControllerGetAnnotationsTest extends IntegrationBase {
    private static final String ENDPOINT_URL = "/hearings/{hearing_id}/annotations";
    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";
    private static final List<String> TAGS_TO_IGNORE = List.of("annotation_id", "uploaded_ts", "hearing_id", "annotation_document_id");

    CustomComparator jsonComparator = new CustomComparator(
        JSONCompareMode.STRICT,
        new Customization("[*].annotation_documents[*].uploaded_ts", (o1, o2) -> true),
        new Customization("[*].hearing_id", (o1, o2) -> true),
        new Customization("[*].annotation_documents.annotation_document_id", (o1, o2) -> true)
    );
    @Autowired
    private transient MockMvc mockMvc;
    @Autowired
    private SuperAdminUserStub superAdminUserStub;
    @MockBean
    private UserIdentity mockUserIdentity;

    @Test
    void hearingNotFound() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, "25");

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void annotationsReturnedJudge() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        AnnotationEntity annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(testUser, "annotationText1");
        annotation.addHearing(hearingEntity);
        dartsDatabase.save(annotation);
        dartsDatabase.getAnnotationStub().createAndSaveAnnotationDocumentEntityWith(
            annotation,
            "filename1",
            "fileType1",
            1001,
            testUser,
            OffsetDateTime.of(
                2020,
                10,
                10,
                10,
                0,
                0,
                0,
                ZoneOffset.UTC
            ),
            "xxxx"
        );

        AnnotationEntity deletedAnnotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(testUser, "annotationText1a");
        deletedAnnotation.addHearing(hearingEntity);
        deletedAnnotation.setDeleted(true);
        dartsDatabase.save(deletedAnnotation);
        dartsDatabase.getAnnotationStub().createAndSaveAnnotationDocumentEntityWith(
            deletedAnnotation,
            "filename1a",
            "fileType1a",
            1001,
            testUser,
            OffsetDateTime.of(
                2020,
                10,
                10,
                10,
                0,
                0,
                0,
                ZoneOffset.UTC
            ),
            "xxxx"
        );


        UserAccountEntity adminUser = dartsDatabase.getUserAccountStub()
            .createSuperAdminUser();
        AnnotationEntity annotation2 = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(adminUser, "annotationText1");
        annotation2.addHearing(hearingEntity);
        dartsDatabase.save(annotation2);
        dartsDatabase.getAnnotationStub().createAndSaveAnnotationDocumentEntityWith(
            annotation2,
            "filename1",
            "fileType1",
            1001,
            adminUser,
            OffsetDateTime.of(
                2020,
                10,
                10,
                10,
                0,
                0,
                0,
                ZoneOffset.UTC
            ),
            "xxxx"
        );


        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, hearingEntity.getId());
        String expected = TestUtils.removeTags(TAGS_TO_IGNORE, """
            [
              {
                "annotation_id": 1,
                "hearing_id": 1,
                "hearing_date": "2023-01-01",
                "annotation_text": "annotationText1",
                "annotation_documents": [
                  {
                    "annotation_document_id": 1,
                    "file_name": "filename1",
                    "file_type": "fileType1",
                    "uploaded_by": "JudgedefaultFullName",
                    "uploaded_ts": "2024-01-31T15:41:04.779601Z"
                  }
                ]
              }
            ]
            """);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        JSONAssert.assertEquals(expected, actualResponse, jsonComparator);
    }

    @Test
    void noAnnotationsReturned_DifferentJudge() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );

        UserAccountEntity thisTestUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser("1");
        when(mockUserIdentity.getUserAccount()).thenReturn(thisTestUser);

        UserAccountEntity otherTestUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser();
        AnnotationEntity annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(otherTestUser, "annotationText1");
        annotation.addHearing(hearingEntity);
        dartsDatabase.save(annotation);
        dartsDatabase.getAnnotationStub().createAndSaveAnnotationDocumentEntityWith(
            annotation,
            "filename1",
            "fileType1",
            1001,
            otherTestUser,
            OffsetDateTime.of(
                2020,
                10,
                10,
                10,
                0,
                0,
                0,
                ZoneOffset.UTC
            ),
            "xxxx"
        );

        UserAccountEntity adminUser = dartsDatabase.getUserAccountStub()
            .createSuperAdminUser();
        AnnotationEntity annotation2 = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(adminUser, "annotationText1");
        annotation2.addHearing(hearingEntity);
        dartsDatabase.save(annotation2);
        dartsDatabase.getAnnotationStub().createAndSaveAnnotationDocumentEntityWith(
            annotation2,
            "filename1",
            "fileType1",
            1001,
            adminUser,
            OffsetDateTime.of(
                2020,
                10,
                10,
                10,
                0,
                0,
                0,
                ZoneOffset.UTC
            ),
            "xxxx"
        );


        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, hearingEntity.getId());
        String expected = TestUtils.removeTags(TAGS_TO_IGNORE, """
            [
            
            ]
            """);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        JSONAssert.assertEquals(expected, actualResponse, jsonComparator);
    }

    @Test
    void allAnnotationsReturned_SuperAdmin() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );


        UserAccountEntity judgeUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser();
        AnnotationEntity annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(judgeUser, "annotationText1");
        annotation.addHearing(hearingEntity);
        dartsDatabase.save(annotation);
        AnnotationDocumentEntity annotationDocumentEntity1 = dartsDatabase.getAnnotationStub().createAndSaveAnnotationDocumentEntityWith(
            annotation,
            "filename1",
            "fileType1",
            1001,
            judgeUser,
            OffsetDateTime.of(
                2020,
                10,
                10,
                10,
                0,
                0,
                0,
                ZoneOffset.UTC
            ),
            "xxxx"
        );

        UserAccountEntity anotherJudgeUser = dartsDatabase.getUserAccountStub()
            .createJudgeUser("2");
        AnnotationEntity annotation2 = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(anotherJudgeUser, "annotationText1");
        annotation2.addHearing(hearingEntity);
        dartsDatabase.save(annotation2);
        AnnotationDocumentEntity annotationDocumentEntity2 = dartsDatabase.getAnnotationStub().createAndSaveAnnotationDocumentEntityWith(
            annotation2,
            "filename1",
            "fileType1",
            1001,
            anotherJudgeUser,
            OffsetDateTime.of(
                2020,
                10,
                10,
                10,
                0,
                0,
                0,
                ZoneOffset.UTC
            ),
            "xxxx"
        );

        UserAccountEntity thisTestUser = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);
        when(mockUserIdentity.getUserAccount()).thenReturn(thisTestUser);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, hearingEntity.getId());
        String expected = TestUtils.removeTags(TAGS_TO_IGNORE, """
                [
                  {
                    "annotation_id": 2,
                    "hearing_id": 1,
                    "hearing_date": "2023-01-01",
                    "annotation_text": "annotationText1",
                    "annotation_documents": [
                      {
                        "annotation_document_id": 2,
                        "file_name": "filename1",
                        "file_type": "fileType1",
                        "uploaded_by": "Judge2FullName",
                        "uploaded_ts": "<<uploaded_ts_doc_2>>"
                      }
                    ]
                  },
                  {
                    "annotation_id": 1,
                    "hearing_id": 1,
                    "hearing_date": "2023-01-01",
                    "annotation_text": "annotationText1",
                    "annotation_documents": [
                      {
                        "annotation_document_id": 1,
                        "file_name": "filename1",
                        "file_type": "fileType1",
                        "uploaded_by": "JudgedefaultFullName",
                        "uploaded_ts": "<<uploaded_ts_doc_1>>"
                      }
                    ]
                  }
                ]
                """)
            .replace("<<uploaded_ts_doc_1>>", annotationDocumentEntity1.getUploadedDateTime().toString())
            .replace("<<uploaded_ts_doc_2>>", annotationDocumentEntity2.getUploadedDateTime().toString());

        assertThat(annotationDocumentEntity2.getUploadedDateTime()).isAfter(annotationDocumentEntity1.getUploadedDateTime());
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());

        JSONAssert.assertEquals(expected, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void fail_NotJudgeOrSuperAdmin() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );


        UserAccountEntity judgeUser = dartsDatabase.getUserAccountStub()
            .createUnauthorisedIntegrationTestUser();
        AnnotationEntity annotation = dartsDatabase.getAnnotationStub().createAndSaveAnnotationEntityWith(judgeUser, "annotationText1");
        annotation.addHearing(hearingEntity);
        dartsDatabase.save(annotation);
        dartsDatabase.getAnnotationStub().createAndSaveAnnotationDocumentEntityWith(
            annotation,
            "filename1",
            "fileType1",
            1001,
            judgeUser,
            OffsetDateTime.of(
                2020,
                10,
                10,
                10,
                0,
                0,
                0,
                ZoneOffset.UTC
            ),
            "xxxx"
        );


        UserAccountEntity thisTestUser = dartsDatabase.getUserAccountStub()
            .createTranscriptionCompanyUser(hearingEntity.getCourtroom().getCourthouse());

        when(mockUserIdentity.getUserAccount()).thenReturn(thisTestUser);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, hearingEntity.getId());
        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isForbidden());
    }
}
