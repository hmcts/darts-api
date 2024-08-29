package uk.gov.hmcts.darts.common.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.service.CourthouseService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseServiceImplTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CourthouseService courthouseService;

    private CaseServiceImpl caseService;

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
        caseService = new CaseServiceImpl(caseRepository, courthouseService);
    }

    @Test
    void retrieveOrCreateCaseWithCourthouseNameExistingCase() {
        when(caseRepository.findByCaseNumberAndCourthouse_CourthouseNameIgnoreCase("CASE123", "Test Courthouse"))
            .thenReturn(Optional.of(existingCase));
        when(caseRepository.saveAndFlush(any(CourtCaseEntity.class))).thenReturn(existingCase);

        CourtCaseEntity result = caseService.retrieveOrCreateCase("Test Courthouse", "CASE123", userAccount);

        assertNotNull(result);
        assertEquals("CASE123", result.getCaseNumber());
        assertEquals(userAccount, result.getLastModifiedBy());
        verify(caseRepository).saveAndFlush(existingCase);
    }

    @Test
    void retrieveOrCreateCaseWithCourthouseNameNewCase() {
        when(caseRepository.findByCaseNumberAndCourthouse_CourthouseNameIgnoreCase("CASE123", "Test Courthouse"))
            .thenReturn(Optional.empty());
        when(courthouseService.retrieveCourthouse("Test Courthouse")).thenReturn(courthouse);
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