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
    public void record(AuditActivity activity, UserAccountEntity userAccountEntity, Optional<CourtCaseEntity> courtCase, Optional<String> additionalData) {
        auditService.recordAudit(activity, userAccountEntity, courtCase, additionalData);
    }

    @Override
    public void record(AuditActivity activity) {
        record(activity, authorisationApi.getCurrentUser());
    }

    @Override
    public void record(AuditActivity activity, String additionalData) {
        auditService.recordAudit(activity, authorisationApi.getCurrentUser(), Optional.empty(), Optional.ofNullable(additionalData));
    }


    @Override
    public void recordAll(AuditActivityProvider auditActivityProvider) {
        auditActivityProvider.getAuditActivities()
            .forEach(auditActivity -> record(auditActivity, authorisationApi.getCurrentUser()));
    }

    @Override
    public void recordAll(AuditActivityProvider auditActivityProvider, CourtCaseEntity courtCase) {
        auditActivityProvider.getAuditActivities()
            .forEach(auditActivity -> record(auditActivity, authorisationApi.getCurrentUser(), courtCase));
    }

}