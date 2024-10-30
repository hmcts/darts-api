package uk.gov.hmcts.darts.casedocument.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.casedocument.model.CourtCaseDocument;
import uk.gov.hmcts.darts.casedocument.service.CaseDocumentService;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentSingleCaseProcessor;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.apache.commons.lang3.CharEncoding.UTF_8;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateCaseDocumentSingleCaseProcessorImpl implements GenerateCaseDocumentSingleCaseProcessor {

    private static final String FILE_NAME_FORMAT = "%s_%s.%s";
    @Value("${darts.case-document.filename-prefix}")
    private String caseDocumentFilenamePrefix;
    @Value("${darts.case-document.file-extension}")
    private String caseDocumentFileExtension;
    @Qualifier("caseDocumentObjectMapper")
    private final ObjectMapper objectMapper;
    private final CaseDocumentRepository caseDocumentRepository;
    private final CaseRepository caseRepository;
    private final CaseDocumentService caseDocumentService;
    private final DataManagementApi dataManagementApi;
    private final DataManagementConfiguration configuration;
    private final ExternalObjectDirectoryService externalObjectDirectoryService;
    private final FileContentChecksum checksumCalculator;
    private final UserIdentity userIdentity;

    @SneakyThrows
    @Override
    @Transactional
    public void processGenerateCaseDocument(Integer caseId) {
        log.info("starting generation of case document json for case id {}", caseId);
        Instant start = Instant.now();

        CourtCaseDocument courtCaseDocument = caseDocumentService.generateCaseDocument(caseId);
        String caseDocumentJson = objectMapper.writeValueAsString(courtCaseDocument);

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        log.info("completed generation of case document json for case id {}. Total execution time (ms): {}", caseId, timeElapsed);
        log.debug("generated case document: {}", caseDocumentJson);

        BlobClientUploadResponse blobClientUploadResponse = dataManagementApi.saveBlobToContainer(
            IOUtils.toInputStream(caseDocumentJson, UTF_8),
            DatastoreContainerType.UNSTRUCTURED
        );
        UUID externalLocation = blobClientUploadResponse.getBlobName();

        var systemUser = userIdentity.getUserAccount();
        CaseDocumentEntity caseDocumentEntity = createAndSaveCaseDocumentEntity(caseId, caseDocumentJson, externalLocation, systemUser);
        externalObjectDirectoryService.createAndSaveCaseDocumentEod(
            externalLocation,
            systemUser,
            caseDocumentEntity,
            EodHelper.unstructuredLocation()
        );
        updateCourtCaseRetention(caseId);
    }

    private void updateCourtCaseRetention(Integer caseId) {
        CourtCaseEntity courtCaseEntity = caseRepository.findById(caseId).get();
        courtCaseEntity.setRetentionUpdated(true);
        courtCaseEntity.setRetentionRetries(0);
        caseRepository.save(courtCaseEntity);
        log.debug("Updated retention for case id {} due to case document generation", caseId);
    }

    private CaseDocumentEntity createAndSaveCaseDocumentEntity(Integer caseId, String caseDocument, UUID externalLocation, UserAccountEntity user) {
        var fileName = String.format(FILE_NAME_FORMAT,
                                     caseDocumentFilenamePrefix,
                                     externalLocation.toString(),
                                     caseDocumentFileExtension
        );
        int fileSize = caseDocument.getBytes().length;
        String checksum = checksumCalculator.calculate(IOUtils.toInputStream(caseDocument, UTF_8));

        CaseDocumentEntity caseDocumentEntity = new CaseDocumentEntity();
        caseDocumentEntity.setCourtCase(caseRepository.getReferenceById(caseId));
        caseDocumentEntity.setFileName(fileName);
        caseDocumentEntity.setChecksum(checksum);
        caseDocumentEntity.setFileSize(fileSize);
        caseDocumentEntity.setFileType(MediaType.APPLICATION_JSON_VALUE);
        caseDocumentEntity.setCreatedBy(user);
        caseDocumentEntity.setLastModifiedBy(user);
        return caseDocumentRepository.save(caseDocumentEntity);
    }
}
