package uk.gov.hmcts.darts.casedocument.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.casedocument.service.CaseDocumentService;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentSingleCaseProcessor;
import uk.gov.hmcts.darts.casedocument.template.CourtCaseDocument;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.common.util.FileContentChecksum;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.util.UUID;

import static org.apache.commons.lang3.CharEncoding.UTF_8;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateCaseSingleCaseProcessorImpl implements GenerateCaseDocumentSingleCaseProcessor {

    @Qualifier("caseDocumentObjectMapper")
    private final ObjectMapper objectMapper;
    private final CaseDocumentRepository caseDocumentRepository;
    private final CaseRepository caseRepository;
    private final CaseDocumentService caseDocumentService;
    private final DataManagementService dataManagementService;
    private final DataManagementConfiguration configuration;
    private final ExternalObjectDirectoryService externalObjectDirectoryService;
    private final FileContentChecksum checksumCalculator;
    private final UserIdentity userIdentity;

    @SneakyThrows
    @Override
    @Transactional
    public void processGenerateCaseDocument(Integer caseId) {
        CourtCaseDocument courtCaseDocument = caseDocumentService.generateCaseDocument(caseId);
        String caseDocumentJson = objectMapper.writeValueAsString(courtCaseDocument);
        log.debug("generated case document: {}", caseDocumentJson);

        UUID externalLocation = dataManagementService.saveBlobData(
            configuration.getUnstructuredContainerName(),
            IOUtils.toInputStream(caseDocumentJson, UTF_8)
        );

        var systemUser = userIdentity.getUserAccount();
        CaseDocumentEntity caseDocumentEntity = createAndSaveCaseDocumentEntity(caseId, caseDocumentJson, externalLocation, systemUser);
        externalObjectDirectoryService.createAndSaveExternalObjectDirectory(
            externalLocation,
            caseDocumentEntity.getChecksum(),
            systemUser,
            caseDocumentEntity,
            EodHelper.unstructuredLocation()
        );
    }

    private CaseDocumentEntity createAndSaveCaseDocumentEntity(Integer caseId, String caseDocument, UUID externalLocation, UserAccountEntity user) {
        int fileSize = caseDocument.getBytes().length;
        String checksum = checksumCalculator.calculate(IOUtils.toInputStream(caseDocument, UTF_8));

        CaseDocumentEntity entity = new CaseDocumentEntity();
        entity.setCourtCase(caseRepository.getReferenceById(caseId));
        entity.setFileName(externalLocation.toString());
        entity.setChecksum(checksum);
        entity.setFileSize(fileSize);
        entity.setFileType("application/json");
        entity.setCreatedBy(user);
        entity.setLastModifiedBy(user);
        return caseDocumentRepository.save(entity);
    }
}
