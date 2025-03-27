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
import uk.gov.hmcts.darts.common.service.CourthouseCommonService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.any;
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
    void retrieveOrCreateCase_WithCourthouseNameExistingCase() {
        when(caseRepository.findByCaseNumberAndCourthouse_CourthouseName("CASE123", "TEST COURTHOUSE"))
            .thenReturn(Optional.of(existingCase));
        when(caseRepository.saveAndFlush(any(CourtCaseEntity.class))).thenReturn(existingCase);

        CourtCaseEntity result = caseService.retrieveOrCreateCase("Test Courthouse", "CASE123", userAccount);

        assertNotNull(result);
        assertEquals("CASE123", result.getCaseNumber());
        assertEquals(userAccount.getId(), result.getLastModifiedById());
        verify(caseRepository).saveAndFlush(existingCase);
    }

    @Test
    void retrieveOrCreateCase_WithCourthouseNameNewCase() {
        when(caseRepository.findByCaseNumberAndCourthouse_CourthouseName("CASE123", "TEST COURTHOUSE"))
            .thenReturn(Optional.empty());
        when(courthouseCommonService.retrieveCourthouse("Test Courthouse")).thenReturn(courthouse);
        when(caseRepository.saveAndFlush(any(CourtCaseEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        CourtCaseEntity result = caseService.retrieveOrCreateCase("Test Courthouse", "CASE123", userAccount);

        assertNotNull(result);
        assertEquals("CASE123", result.getCaseNumber());
        assertEquals(courthouse, result.getCourthouse());
        assertEquals(userAccount, result.getCreatedBy());
        assertEquals(userAccount.getId(), result.getLastModifiedById());
        assertFalse(result.getClosed());
        assertFalse(result.getInterpreterUsed());
        verify(caseRepository).saveAndFlush(any(CourtCaseEntity.class));
    }

    @Test
    void retrieveOrCreateCase_WithCourthouseExistingCase() {
        when(caseRepository.findByCaseNumberAndCourthouse("CASE123", courthouse))
            .thenReturn(Optional.of(existingCase));
        when(caseRepository.saveAndFlush(any(CourtCaseEntity.class))).thenReturn(existingCase);

        CourtCaseEntity result = caseService.retrieveOrCreateCase(courthouse, "CASE123", userAccount);

        assertNotNull(result);
        assertEquals("CASE123", result.getCaseNumber());
        assertEquals(userAccount.getId(), result.getLastModifiedById());
        verify(caseRepository).saveAndFlush(existingCase);
    }

    @Test
    void retrieveOrCreateCase_WithCourthouseNewCase() {
        when(caseRepository.findByCaseNumberAndCourthouse("CASE123", courthouse))
            .thenReturn(Optional.empty());
        when(caseRepository.saveAndFlush(any(CourtCaseEntity.class))).thenAnswer(i -> i.getArguments()[0]);

        CourtCaseEntity result = caseService.retrieveOrCreateCase(courthouse, "CASE123", userAccount);

        assertNotNull(result);
        assertEquals("CASE123", result.getCaseNumber());
        assertEquals(courthouse, result.getCourthouse());
        assertEquals(userAccount, result.getCreatedBy());
        assertEquals(userAccount.getId(), result.getLastModifiedById());
        assertFalse(result.getClosed());
        assertFalse(result.getInterpreterUsed());
        verify(caseRepository).saveAndFlush(any(CourtCaseEntity.class));
    }

    @Test
    void retrieveOrCreateCase_ReturnsCase_UsingWhitespaceCourthouseName() {
        // given
        when(caseRepository.findByCaseNumberAndCourthouse_CourthouseName("CASE123", "TEST COURTHOUSE"))
            .thenReturn(Optional.of(existingCase));
        when(caseRepository.saveAndFlush(any(CourtCaseEntity.class))).thenReturn(existingCase);

        // when
        CourtCaseEntity result = caseService.retrieveOrCreateCase(" Test Courthouse ", "CASE123", userAccount);

        // then
        assertNotNull(result);
        assertEquals("CASE123", result.getCaseNumber());
        assertEquals("TEST COURTHOUSE", result.getCourthouse().getCourthouseName());
        assertEquals(userAccount.getId(), result.getLastModifiedById());
        verify(caseRepository).saveAndFlush(existingCase);
    }
}