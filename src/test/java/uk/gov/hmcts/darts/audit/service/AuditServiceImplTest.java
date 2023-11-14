package uk.gov.hmcts.darts.audit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.enums.AuditActivityEnum;
import uk.gov.hmcts.darts.audit.service.impl.AuditServiceImpl;
import uk.gov.hmcts.darts.common.entity.AuditActivityEntity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AuditActivityRepository;
import uk.gov.hmcts.darts.common.repository.AuditRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.LawOfDemeter")
class AuditServiceImplTest {
    @Mock
    AuditActivityRepository auditActivityRepository;
    @Mock
    AuditRepository auditRepository;
    @Mock
    UserAccountRepository userAccountRepository;
    @Mock
    HearingRepository hearingRepository;

    AuditServiceImpl auditServiceImpl;

    @Captor
    ArgumentCaptor<AuditEntity> auditEntityArgumentCaptor;

    @BeforeEach
    void setUp() {
        auditServiceImpl = new AuditServiceImpl(
            auditActivityRepository,
            auditRepository,
            userAccountRepository,
            hearingRepository
        );
    }

    @Test
    void testRecordAudit() {
        AuditActivityEntity auditActivityEntity = new AuditActivityEntity();
        auditActivityEntity.setName(String.valueOf(AuditActivityEnum.MOVE_COURTROOM));
        when(auditActivityRepository.getReferenceById(any())).thenReturn(auditActivityEntity);

        auditServiceImpl.recordAudit(AuditActivityEnum.MOVE_COURTROOM, new UserAccountEntity(), new CourtCaseEntity());

        verify(auditRepository).saveAndFlush(auditEntityArgumentCaptor.capture());

        AuditEntity savedValue = auditEntityArgumentCaptor.getValue();
        assertNotNull(savedValue.getCourtCase());
        assertNotNull(savedValue.getUser());
        assertNotNull(savedValue.getApplicationServer());
        assertNotNull(savedValue.getCreatedBy());
        assertNotNull(savedValue.getLastModifiedBy());
        assertNull(savedValue.getAdditionalData());
        assertEquals(String.valueOf(AuditActivityEnum.MOVE_COURTROOM), savedValue.getAuditActivity().getName());
    }

}
