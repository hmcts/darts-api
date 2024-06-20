package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentProcessor;
import uk.gov.hmcts.darts.casedocument.service.GenerateCaseDocumentSingleCaseDocumentProcessor;
import uk.gov.hmcts.darts.casedocument.service.impl.GenerateCaseDocumentBatchProcessorImpl;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public class GenerateCaseDocumentProcessorIntTest extends IntegrationBase {

    @SpyBean
    CaseRepository caseRepository;
    @SpyBean
    CaseDocumentRepository caseDocumentRepository;
    @SpyBean
    ExternalObjectDirectoryRepository eodRepository;
    @Autowired
    GenerateCaseDocumentSingleCaseDocumentProcessor singleCaseProcessor;
    @Autowired
    CurrentTimeHelper currentTimeHelper;

    @Test
    public void test() {

        // given
        var batchSize = 10;
        GenerateCaseDocumentProcessor generateCaseDocumentProcessor = new GenerateCaseDocumentBatchProcessorImpl(
            batchSize, caseRepository, singleCaseProcessor, currentTimeHelper);

        CourtCaseEntity courtCaseEntity = dartsDatabase.getCourtCaseStub().createCourtCaseAndAssociatedEntitiesWithRandomValues();
        when(caseRepository.findCasesNeedingCaseDocumentGenerated(any(), any())).thenReturn(List.of(courtCaseEntity));
        when(caseRepository.findById(anyInt())).thenReturn(Optional.of(courtCaseEntity));

        ExternalObjectDirectoryEntity mediaEodEntity = dartsDatabase.getExternalObjectDirectoryStub().createEodWithRandomValues();
        when(eodRepository.findByMedia(any())).thenReturn(List.of(mediaEodEntity));

        ExternalObjectDirectoryEntity transcriptionDocumentEodEntity = dartsDatabase.getExternalObjectDirectoryStub().createEodWithRandomValues();
        when(eodRepository.findByTranscriptionDocumentEntity(any())).thenReturn(List.of(transcriptionDocumentEodEntity));

        ExternalObjectDirectoryEntity annotationDocumentEodEntity = dartsDatabase.getExternalObjectDirectoryStub().createEodWithRandomValues();
        when(eodRepository.findByAnnotationDocumentEntity(any())).thenReturn(List.of(annotationDocumentEodEntity));

        when(caseDocumentRepository.findByCourtCase(any())).thenReturn(List.of(dartsDatabase.getCaseDocumentStub().createCaseDocumentWithRandomValues()));
        ExternalObjectDirectoryEntity caseDocumentEodEntity = dartsDatabase.getExternalObjectDirectoryStub().createEodWithRandomValues();
        when(eodRepository.findByCaseDocument(any())).thenReturn(List.of(caseDocumentEodEntity));

        // when
        generateCaseDocumentProcessor.processGenerateCaseDocument();


    }
}
