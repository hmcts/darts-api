package uk.gov.hmcts.darts.annotation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.datamanagement.api.DataManagementFacade;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.InputStream;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;
import static uk.gov.hmcts.darts.test.common.data.AnnotationTestData.minimalAnnotationEntity;
import static uk.gov.hmcts.darts.test.common.data.HearingTestData.someMinimalHearing;

@Disabled("Impacted by V1_365__adding_not_null_constraints_part_4.sql")
@AutoConfigureMockMvc
@Disabled("Impacted by V1_364_*.sql")
class AnnotationGetTest extends IntegrationBase {

    private static final String ANNOTATION_DOCUMENT_ENDPOINT = "/annotations/{annotation_id}/documents/{annotation_document_id}";

    @Mock
    private InputStream inputStreamResource;

    @Mock
    private DownloadResponseMetaData downloadResponseMetaData;
    @MockBean
    private DataManagementFacade dataManagementFacade;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GivenBuilder given;


    @Test
    void shouldThrowHttp404ForValidJudgeAndInvalidAnnotationDocumentEntity() throws Exception {

        AnnotationEntity uae = someAnnotationCreatedBy(given.anAuthenticatedUserWithGlobalAccessAndRole(JUDICIARY));

        MockHttpServletRequestBuilder requestBuilder = get(ANNOTATION_DOCUMENT_ENDPOINT, uae.getId(), -1);

        mockMvc.perform(
                requestBuilder)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value("ANNOTATION_103"))
            .andExpect(jsonPath("$.title").value("invalid annotation id or annotation document id"))
            .andExpect(jsonPath("$.status").value("404"))
            .andReturn();
    }


    @Test
    void shouldDownloadAnnotationDocument() throws Exception {

        var judge = given.anAuthenticatedUserWithGlobalAccessAndRole(JUDICIARY);
        when(downloadResponseMetaData.getInputStream()).thenReturn(inputStreamResource);
        when(dataManagementFacade.retrieveFileFromStorage(anyList())).thenReturn(downloadResponseMetaData);

        var annotationDocument = dartsDatabase.createValidAnnotationDocumentForDownload(judge);

        MockHttpServletRequestBuilder requestBuilder = get(ANNOTATION_DOCUMENT_ENDPOINT,
                                                           annotationDocument.getAnnotation().getId(),
                                                           annotationDocument.getId());


        mockMvc.perform(
                requestBuilder)
            .andExpect(status().isOk())
            .andExpect(header().string(
                CONTENT_DISPOSITION,
                "attachment; filename=\"judges-notes.txt\""
            ))
            .andExpect(header().string(
                CONTENT_TYPE,
                "application/zip"
            ))
            .andExpect(header().string(
                "annotation_document_id",
                "1"
            ));

    }

    private AnnotationEntity someAnnotationCreatedBy(UserAccountEntity userAccount) {
        var annotation = minimalAnnotationEntity();
        annotation.setDeleted(false);
        annotation.setCurrentOwner(userAccount);
        annotation.setCreatedBy(userAccount);
        annotation.setLastModifiedBy(userAccount);
        annotation.addHearing(dartsDatabase.save(someMinimalHearing()));
        dartsDatabase.save(annotation);
        return annotation;
    }

}
