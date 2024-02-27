package uk.gov.hmcts.darts.annotation;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.InputStream;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.testutils.data.AnnotationTestData.minimalAnnotationEntity;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.someMinimalHearing;

@AutoConfigureMockMvc
class AnnotationGetTest extends IntegrationBase {

    private static final String ANNOTATION_DOCUMENT_ENDPOINT = "/annotations/{annotation_id}/documents/{annotation_document_id}";

    @Mock
    private DataManagementApi dataManagementApi;

    @Mock
    private InputStream inputStreamResource;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnnotationTestGivensBuilder given;


    @Test
    void shouldThrowHttp404ForValidJudgeAndInvalidAnnotationDocumentEntity() throws Exception {

        AnnotationEntity uae = someAnnotationCreatedBy(given.anAuthenticatedUserWithGlobalAccessAndRole(JUDGE));

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

        var judge = given.anAuthenticatedUserWithGlobalAccessAndRole(JUDGE);

        var binaryData = mock(BinaryData.class);
        lenient().when(dataManagementApi.getBlobDataFromUnstructuredContainer(any(UUID.class))).thenReturn(binaryData);
        lenient().when(binaryData.toStream()).thenReturn(inputStreamResource);

        dartsDatabase.createValidAnnotationDocumentForDownload(judge);

        MockHttpServletRequestBuilder requestBuilder = get(ANNOTATION_DOCUMENT_ENDPOINT, 1, 1);

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
                        "external_location",
                        "665e00c8-5b82-4392-8766-e0c982f603d3"
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
        annotation.addHearing(dartsDatabase.save(someMinimalHearing()));
        dartsDatabase.save(annotation);
        return annotation;
    }

}
