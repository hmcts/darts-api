package uk.gov.hmcts.darts.casedocument.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.casedocument.model.CourtCaseDocument;
import uk.gov.hmcts.darts.casedocument.service.CaseDocumentService;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateCaseDocumentSingleCaseProcessorImplTest {

    private static final int CASE_ID = 44;
    private static final String UNSTRUCTURED_CONTAINER_NAME = "unstructured";
    private static final UUID UNSTRUCTURED_BLOB_UUID = UUID.randomUUID();
    private static final String CHECKSUM = "ssUxli";
    private static final String CASE_DOCUMENT_JSON = """
        {"createdDateTime":"2029-12-12T13:19:01.698514618Z", ...}
        """;

    @Mock
    ObjectMapper objectMapper;
    @Mock
    CaseDocumentRepository caseDocumentRepository;
    @Mock
    CaseRepository caseRepository;
    @Mock
    CaseDocumentService caseDocumentService;
    @Mock
    DataManagementService dataManagementService;
    @Mock
    DataManagementConfiguration configuration;
    @Mock
    ExternalObjectDirectoryService externalObjectDirectoryService;
    @Mock
    FileContentChecksum checksumCalculator;
    @Mock
    UserIdentity userIdentity;

    @InjectMocks
    GenerateCaseDocumentSingleCaseProcessorImpl processor;

    @Mock
    CourtCaseDocument courtCaseDocument;
    @Mock
    UserAccountEntity user;
    @Mock
    CaseDocumentEntity caseDocumentEntity;
    @Mock
    CourtCaseEntity caseEntity;

    @Captor
    ArgumentCaptor<CaseDocumentEntity> caseDocumentCaptor;

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    @AfterAll
    public static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @BeforeEach
    void setup() {
        when(configuration.getUnstructuredContainerName()).thenReturn(UNSTRUCTURED_CONTAINER_NAME);
    }

    @SneakyThrows
    @Test
    void testGenerationOfCaseDocument() {

        // given
        when(caseDocumentService.generateCaseDocument(CASE_ID)).thenReturn(courtCaseDocument);
        when(objectMapper.writeValueAsString(courtCaseDocument)).thenReturn(CASE_DOCUMENT_JSON);
        when(dataManagementService.saveBlobData(eq(UNSTRUCTURED_CONTAINER_NAME), any(InputStream.class))).thenReturn(UNSTRUCTURED_BLOB_UUID);
        when(userIdentity.getUserAccount()).thenReturn(user);
        when(caseDocumentRepository.save(any())).thenReturn(caseDocumentEntity);
        when(checksumCalculator.calculate(any(InputStream.class))).thenReturn(CHECKSUM);
        when(caseRepository.getReferenceById(CASE_ID)).thenReturn(caseEntity);
        // when
        processor.processGenerateCaseDocument(CASE_ID);

        // then
        verify(externalObjectDirectoryService).createAndSaveCaseDocumentEod(
            UNSTRUCTURED_BLOB_UUID,
            user,
            caseDocumentEntity,
            EodHelper.unstructuredLocation());
        verify(caseDocumentRepository).save(caseDocumentCaptor.capture());
        CaseDocumentEntity savedCaseDocument = caseDocumentCaptor.getValue();
        assertThat(savedCaseDocument.getCourtCase()).isEqualTo(caseEntity);
        assertThat(savedCaseDocument.getChecksum()).isEqualTo(CHECKSUM);
        assertThat(savedCaseDocument.getFileName()).isEqualTo(UNSTRUCTURED_BLOB_UUID.toString());
        assertThat(savedCaseDocument.getFileSize()).isEqualTo(CASE_DOCUMENT_JSON.getBytes(StandardCharsets.UTF_8).length);
        assertThat(savedCaseDocument.getFileType()).isEqualTo("application/json");
        assertThat(savedCaseDocument.getCreatedBy()).isEqualTo(user);
        assertThat(savedCaseDocument.getLastModifiedBy()).isEqualTo(user);
        assertThat(savedCaseDocument.isHidden()).isEqualTo(false);
    }

}