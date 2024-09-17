package uk.gov.hmcts.darts.common.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.service.CourthouseCommonService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseCommonServiceImplTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CourthouseCommonService courthouseCommonService;

    private CaseCommonServiceImpl caseService;

    private UserAccountEntity userAccount;
    private CourthouseEntity courthouse;
    private CourtCaseEntity existingCase;

    @BeforeEach
    void setUp() {
        userAccount = new UserAccountEntity();
        courthouse = new CourthouseEntity();
        courthouse.setCourthouseName("Test Courthouse");
        existingCase = new CourtCaseEntity();
        existingCase.setCaseNumber("CASE123");
        existingCase.setCourthouse(courthouse);
        caseService = new CaseCommonServiceImpl(caseRepository, courthouseCommonService);
    }

    @Test
    void retrieveOrCreateCaseWithCourthouseNameExistingCase() {
        when(caseRepository.findByCaseNumberAndCourthouse_CourthouseName("CASE123", "TEST COURTHOUSE"))
            .thenReturn(Optional.of(existingCase));
        when(caseRepository.saveAndFlush(any(CourtCaseEntity.class))).thenReturn(existingCase);

        CourtCaseEntity result = caseService.retrieveOrCreateCase("Test Courthouse", "CASE123", userAccount);

        assertNotNull(result);
        assertEquals("CASE123", result.getCaseNumber());
        assertEquals(userAccount, result.getLastModifiedBy());
        verify(caseRepository).saveAndFlush(existingCase);
    }

    @Test
    void retrieveOrCreateCaseWithCourthouseNameExistingExpiredCase() {
        when(caseRepository.findByCaseNumberAndCourthouse_CourthouseName("CASE123", "TEST COURTHOUSE"))
            .thenReturn(Optional.of(existingCase));
        existingCase.setDataAnonymised(true);
        doAnswer(i -> i.getArguments()[0]).when(caseRepository).saveAndFlush(any(CourtCaseEntity.class));

        DartsApiException exception = assertThrows(
            DartsApiException.class, () -> caseService.retrieveOrCreateCase("Test Courthouse", "CASE123", userAccount));

        assertThat(exception.getError()).isEqualTo(CaseApiError.CASE_EXPIRED);
        assertThat(exception.getMessage()).isEqualTo("Case has expired.");
    }

    @Test
    void retrieveOrCreateCaseWithCourthouseNameNewCase() {
        when(caseRepository.findByCaseNumberAndCourthouse_CourthouseName("CASE123", "TEST COURTHOUSE"))
            .thenReturn(Optional.empty());
        when(courthouseCommonService.retrieveCourthouse("Test Courthouse")).thenReturn(courthouse);
        when(caseRepository.saveAndFlush(any(CourtCaseEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        CourtCaseEntity result = caseService.retrieveOrCreateCase("Test Courthouse", "CASE123", userAccount);

        assertNotNull(result);
        assertEquals("CASE123", result.getCaseNumber());
        assertEquals(courthouse, result.getCourthouse());
        assertEquals(userAccount, result.getCreatedBy());
        assertEquals(userAccount, result.getLastModifiedBy());
        assertFalse(result.getClosed());
        assertFalse(result.getInterpreterUsed());
        verify(caseRepository).saveAndFlush(any(CourtCaseEntity.class));
    }

    @Test
    void retrieveOrCreateCaseWithCourthouseExistingCase() {
        when(caseRepository.findByCaseNumberAndCourthouse("CASE123", courthouse))
            .thenReturn(Optional.of(existingCase));
        when(caseRepository.saveAndFlush(any(CourtCaseEntity.class))).thenReturn(existingCase);

        CourtCaseEntity result = caseService.retrieveOrCreateCase(courthouse, "CASE123", userAccount);

        assertNotNull(result);
        assertEquals("CASE123", result.getCaseNumber());
        assertEquals(userAccount, result.getLastModifiedBy());
        verify(caseRepository).saveAndFlush(existingCase);
    }

    @Test
    void retrieveOrCreateCaseWithCourthouseExistingCaseIsExpired() {
        when(caseRepository.findByCaseNumberAndCourthouse("CASE123", courthouse))
            .thenReturn(Optional.of(existingCase));
        existingCase.setDataAnonymised(true);
        when(caseRepository.saveAndFlush(any(CourtCaseEntity.class))).thenReturn(existingCase);

        DartsApiException exception = assertThrows(
            DartsApiException.class, () -> caseService.retrieveOrCreateCase(courthouse, "CASE123", userAccount));

        assertThat(exception.getError()).isEqualTo(CaseApiError.CASE_EXPIRED);
        assertThat(exception.getMessage()).isEqualTo("Case has expired.");
    }

    @Test
    void retrieveOrCreateCaseWithCourthouseNewCase() {
        when(caseRepository.findByCaseNumberAndCourthouse("CASE123", courthouse))
            .thenReturn(Optional.empty());
        when(caseRepository.saveAndFlush(any(CourtCaseEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        CourtCaseEntity result = caseService.retrieveOrCreateCase(courthouse, "CASE123", userAccount);

        assertNotNull(result);
        assertEquals("CASE123", result.getCaseNumber());
        assertEquals(courthouse, result.getCourthouse());
        assertEquals(userAccount, result.getCreatedBy());
        assertEquals(userAccount, result.getLastModifiedBy());
        assertFalse(result.getClosed());
        assertFalse(result.getInterpreterUsed());
        verify(caseRepository).saveAndFlush(any(CourtCaseEntity.class));
    }
}