package uk.gov.hmcts.darts.audit.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditActivityProvider;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.audit.service.AuditService;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuditApiImpl implements AuditApi {

    private final AuditService auditService;
    private final AuthorisationApi authorisationApi;

    @Override
    public void record(AuditActivity activity, UserAccountEntity userAccountEntity, CourtCaseEntity courtCase, String additionalData) {
        auditService.recordAudit(activity, userAccountEntity, courtCase == null ? Optional.empty() : Optional.of(courtCase), additionalData == null ? Optional.empty() : Optional.of(additionalData));
    }

    @Override
    public void record(AuditActivity activity) {
        auditService.recordAudit(activity, authorisationApi.getCurrentUser(), Optional.empty(), Optional.empty());
    }

    @Override
    public void recordAll(AuditActivityProvider auditActivityProvider) {
        auditActivityProvider.getAuditActivities()
            .forEach(auditActivity -> record(auditActivity, authorisationApi.getCurrentUser(), null, null));
    }

    @Override
    public void recordAll(AuditActivityProvider auditActivityProvider, CourtCaseEntity courtCase) {
        auditActivityProvider.getAuditActivities()
            .forEach(auditActivity -> record(auditActivity, authorisationApi.getCurrentUser(), courtCase));
    }

}