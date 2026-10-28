package uk.gov.hmcts.darts.cases.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.cases.exception.CaseLinkRowException;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.service.CaseCommonService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseLinkRowServiceTest {

    @Mock
    private CourthouseRepository courthouseRepository;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private CaseCommonService caseCommonService;
    @Mock
    private CaseLinkedCaseRepository linkedCaseRepository;
    @Mock
    private CaseLinkImportResultRepository resultRepository;
    @InjectMocks
    private CaseLinkRowService service;

    private final CourthouseEntity courthouse = new CourthouseEntity();
    private final UserAccountEntity user = new UserAccountEntity();
    private final CaseLinkImportResultEntity row = new CaseLinkImportResultEntity();

    @BeforeEach
    void setUp() {
        user.setId(123);
        row.setCase1CaseNumber(" X1 ");
        row.setCase1Courthouse(" york ");
        row.setCase2CaseNumber(" CP1 ");
    }

    @Test
    void given_missingCourthouse_when_processing_then_failWithoutCreatingCase() {
        when(courthouseRepository.findForCaseLinkImport("YORK")).thenReturn(Optional.empty());
        assertFailure("COURTHOUSE_NOT_FOUND");
        verify(caseCommonService, never()).retrieveOrCreateCase(any(CourthouseEntity.class), any(), any());
    }

    @Test
    void given_missingCase1_when_processing_then_failWithoutCreatingCase2() {
        when(courthouseRepository.findForCaseLinkImport("YORK")).thenReturn(Optional.of(courthouse));
        when(caseRepository.findByCaseNumberAndCourthouse("X1", courthouse)).thenReturn(Optional.empty());
        assertFailure("CASE_1_NOT_FOUND");
        verify(caseCommonService, never()).retrieveOrCreateCase(any(CourthouseEntity.class), any(), any());
    }

    @Test
    void given_missingCase2_when_processing_then_useExistingCreationCodeAndCreateLink() {
        var first = givenFirstCase();
        var second = courtCase(2);
        when(caseRepository.findByCaseNumberAndCourthouse("CP1", courthouse)).thenReturn(Optional.empty());
        when(caseCommonService.retrieveOrCreateCase(courthouse, "CP1", user)).thenReturn(second);
        when(linkedCaseRepository.findByCaseIds(1, 2)).thenReturn(List.of());
        when(linkedCaseRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            CaseLinkedCaseEntity link = invocation.getArgument(0);
            assertThat(link.getCourtCase1()).isSameAs(first);
            assertThat(link.getCourtCase2()).isSameAs(second);
            assertThat(link.getCreatedById()).isEqualTo(user.getId());
            link.setId(10);
            return link;
        });
        service.process(row, user);
        assertThat(row.isCase2Created()).isTrue();
        assertThat(row.getCase2Id()).isEqualTo(2);
        assertThat(row.getLinkedCaseId()).isEqualTo(10);
        assertThat(row.getStatus().getId()).isEqualTo(CaseLinkResultStatus.SUCCESS.getId());
        verify(resultRepository).saveAndFlush(row);
    }

    @Test
    void given_existingCaseAndLink_when_processing_then_reuseWithoutCreatingOrUpdatingCases() {
        givenFirstCase();
        when(caseRepository.findByCaseNumberAndCourthouse("CP1", courthouse)).thenReturn(Optional.of(courtCase(2)));
        var link = new CaseLinkedCaseEntity();
        link.setId(10);
        when(linkedCaseRepository.findByCaseIds(1, 2)).thenReturn(List.of(link));
        service.process(row, user);
        assertThat(row.isCase2Created()).isFalse();
        assertThat(row.getLinkedCaseId()).isEqualTo(10);
        verify(caseCommonService, never()).retrieveOrCreateCase(any(CourthouseEntity.class), any(), any());
        verify(linkedCaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void given_sameCaseIds_when_processing_then_rejectSelfLink() {
        var first = givenFirstCase();
        when(caseRepository.findByCaseNumberAndCourthouse("CP1", courthouse)).thenReturn(Optional.of(first));
        assertFailure("SELF_LINK");
        verify(linkedCaseRepository, never()).saveAndFlush(any());
    }

    @Test
    void given_deletedAndAnonymisedCases_when_processing_then_recordWarningsAndContinue() {
        var first = givenFirstCase();
        first.setDeleted(true);
        var second = courtCase(2);
        second.setDataAnonymised(true);
        when(caseRepository.findByCaseNumberAndCourthouse("CP1", courthouse)).thenReturn(Optional.of(second));
        when(linkedCaseRepository.findByCaseIds(1, 2)).thenReturn(List.of(new CaseLinkedCaseEntity()));
        service.process(row, user);
        assertThat(row.getWarningCodes()).isEqualTo("CASE_1_DELETED,CASE_2_ANONYMISED");
        assertThat(row.getStatus().getId()).isEqualTo(CaseLinkResultStatus.SUCCESS.getId());
    }

    private CourtCaseEntity givenFirstCase() {
        var first = courtCase(1);
        when(courthouseRepository.findForCaseLinkImport("YORK")).thenReturn(Optional.of(courthouse));
        when(caseRepository.findByCaseNumberAndCourthouse("X1", courthouse)).thenReturn(Optional.of(first));
        return first;
    }

    private CourtCaseEntity courtCase(int id) {
        var entity = new CourtCaseEntity();
        entity.setId(id);
        return entity;
    }

    private void assertFailure(String code) {
        assertThatThrownBy(() -> service.process(row, user)).isInstanceOfSatisfying(CaseLinkRowException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(code));
    }
}
