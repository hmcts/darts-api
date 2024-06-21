package uk.gov.hmcts.darts.casedocument.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.casedocument.mapper.CourtCaseDocumentMapper;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseDocumentServiceImplTest {

    public static final int CASE_ID = 44;

    @Mock CaseRepository caseRepository;
    @Mock CourtCaseDocumentMapper caseDocumentMapper;
    @Mock
    CourtCaseEntity courtCase;

    CaseDocumentServiceImpl caseDocumentService;

    @BeforeEach
    void setup() {
        caseDocumentService = new CaseDocumentServiceImpl(caseRepository, caseDocumentMapper);
    }

    @Test
    void testGenerateCaseDocument() {

        when(caseRepository.findById(CASE_ID)).thenReturn(Optional.of(courtCase));

        caseDocumentService.generateCaseDocument(CASE_ID);

        verify(caseDocumentMapper).mapToCaseDocument(courtCase);
    }

    @Test
    void testThrowsExceptionIfCaseNotFound() {

        when(caseRepository.findById(CASE_ID)).thenReturn(Optional.empty());

        Assertions.assertThrows(DartsException.class,
                                () -> caseDocumentService.generateCaseDocument(CASE_ID));

    }
}