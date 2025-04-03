package uk.gov.hmcts.darts.annotation;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.Resource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.datamanagement.api.DataManagementFacade;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDICIARY;

@AutoConfigureMockMvc
class AnnotationGetTest extends IntegrationBase {

    private static final String ANNOTATION_DOCUMENT_ENDPOINT = "/annotations/{annotation_id}/documents/{annotation_document_id}";

    @Mock
    private InputStream inputStreamResource;

    @Mock
    private DownloadResponseMetaData downloadResponseMetaData;
    @MockitoBean
    private DataManagementFacade dataManagementFacade;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GivenBuilder given;


    @Test
    void shouldThrowHttp404ForValidJudgeAndInvalidAnnotationDocumentEntity() throws Exception {
        var judge = given.anAuthenticatedUserWithGlobalAccessAndRole(JUDICIARY);
        var uae = someAnnotationCreatedBy(judge);

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

        Resource resource = Mockito.mock(Resource.class);
        when(downloadResponseMetaData.getResource()).thenReturn(resource);

        when(downloadResponseMetaData.getResource()).thenReturn(resource);
        when(dataManagementFacade.retrieveFileFromStorage(anyList())).thenReturn(downloadResponseMetaData);

        var annotationDocument = createValidAnnotationDocumentForDownload(judge);

        MockHttpServletRequestBuilder requestBuilder = get(ANNOTATION_DOCUMENT_ENDPOINT,
                                                           annotationDocument.getAnnotation().getId(),
                                                           annotationDocument.getId());

        mockMvc.perform(
                requestBuilder)
            .andExpect(status().isOk())
            .andExpect(header().string(
                CONTENT_DISPOSITION,
                "attachment; filename=\"judges-notes.txt\""))
            .andExpect(header().string(
                CONTENT_TYPE,
                "application/zip"))
            .andExpect(header().string(
                "annotation_document_id",
                "1"));

    }

    private AnnotationDocumentEntity createValidAnnotationDocumentForDownload(UserAccountEntity judge) {
        var annotation = someAnnotationCreatedBy(judge);

        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = OffsetDateTime.now();
        final String checksum = "123";
        var annotationDocumentEntity = dartsDatabase.getAnnotationStub()
            .createAndSaveAnnotationDocumentEntityWith(annotation, fileName, fileType, fileSize,
                                                       judge, uploadedDateTime, checksum);

        var armEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            annotationDocumentEntity,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            "665e00c8-5b82-4392-8766-e0c982f603d3"
        );
        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod);

        var armEod2 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            annotationDocumentEntity,
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(ExternalLocationTypeEnum.ARM),
            "665e00c8-5b82-4392-8766-e0c982f603d3"
        );
        armEod.setTransferAttempts(1);
        dartsDatabase.save(armEod2);

        return annotationDocumentEntity;
    }

    private AnnotationEntity someAnnotationCreatedBy(UserAccountEntity userAccount) {
        var annotation = PersistableFactory.getAnnotationTestData().someMinimalBuilder()
            .deleted(false)
            .currentOwner(userAccount)
            .createdById(0)
            .lastModifiedById(0)
            .hearingList(new ArrayList<>(List.of(PersistableFactory.getHearingTestData().someMinimalHearing())));

        return dartsPersistence.save(annotation.build().getEntity());
    }

}