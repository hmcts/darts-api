package uk.gov.hmcts.darts.cases.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CaseLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.repository.CaseLinkedCaseRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseLinkedCaseServiceImplTest {

    @Mock
    private CaseLinkedCaseRepository caseLinkedCaseRepository;

    private CaseLinkedCaseServiceImpl caseLinkedCaseService;

    @BeforeEach
    void setUp() {
        caseLinkedCaseService = new CaseLinkedCaseServiceImpl(caseLinkedCaseRepository);
    }

    @Test
    void getLinkedCases_shouldReturnLinkedCaseEntities() {
        CourtCaseEntity courtCase = CommonTestDataUtil.createCaseWithId("case", 101);
        CourtCaseEntity linkedCase1 = CommonTestDataUtil.createCaseWithId("linkedCase1", 201);
        CourtCaseEntity linkedCase2 = CommonTestDataUtil.createCaseWithId("linkedCase2", 202);
        CaseLinkedCaseEntity linkedCaseEntity1 = createLinkedCase(courtCase, linkedCase1);
        CaseLinkedCaseEntity linkedCaseEntity2 = createLinkedCase(linkedCase2, courtCase);
        when(caseLinkedCaseRepository.findByCourtCase(courtCase)).thenReturn(List.of(linkedCaseEntity1, linkedCaseEntity2));

        List<CaseLinkedCaseEntity> result = caseLinkedCaseService.getLinkedCases(courtCase);

        assertThat(result).containsExactly(linkedCaseEntity1, linkedCaseEntity2);
        verify(caseLinkedCaseRepository).findByCourtCase(courtCase);
    }

    @Test
    void getLinkedCasesByCourtCases_shouldReturnEmptyList_whenNoCourtCasesProvided() {
        List<CaseLinkedCaseEntity> result = caseLinkedCaseService.getLinkedCasesByCourtCases(List.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(caseLinkedCaseRepository);
    }

    @Test
    void getLinkedCasesByCourtCases_shouldReturnLinkedCaseEntities_whenCourtCasesProvided() {
        CourtCaseEntity courtCase = CommonTestDataUtil.createCaseWithId("case", 101);
        CourtCaseEntity linkedCase = CommonTestDataUtil.createCaseWithId("linkedCase", 201);
        CaseLinkedCaseEntity linkedCaseEntity = createLinkedCase(courtCase, linkedCase);
        List<CourtCaseEntity> courtCases = List.of(courtCase);
        when(caseLinkedCaseRepository.findByCourtCaseIn(courtCases)).thenReturn(List.of(linkedCaseEntity));

        List<CaseLinkedCaseEntity> result = caseLinkedCaseService.getLinkedCasesByCourtCases(courtCases);

        assertThat(result).containsExactly(linkedCaseEntity);
        verify(caseLinkedCaseRepository).findByCourtCaseIn(courtCases);
    }

    private CaseLinkedCaseEntity createLinkedCase(CourtCaseEntity courtCase, CourtCaseEntity linkedCase) {
        CaseLinkedCaseEntity linkedCaseEntity = new CaseLinkedCaseEntity();
        linkedCaseEntity.setCourtCase1(courtCase);
        linkedCaseEntity.setCourtCase2(linkedCase);
        return linkedCaseEntity;
    }
}
