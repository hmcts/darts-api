package uk.gov.hmcts.darts.audit.service;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.RevisionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.darts.audit.model.RevisionInfo;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;

import java.util.Optional;


@Slf4j
public class AuditorRevisionListener implements RevisionListener {
    private final UserIdentity userIdentity;

    @Autowired
    public AuditorRevisionListener(@Lazy UserIdentity userIdentity) {
        this.userIdentity = userIdentity;
    }

    @Override
    public void newRevision(Object revisionEntity) {
        if (revisionEntity instanceof RevisionInfo revisionInfo) {
            Optional<Integer> auditor = getUserAccount();
            auditor.ifPresent(revisionInfo::setAuditUser);
        }
    }

    Optional<Integer> getUserAccount() {
        try {
            return userIdentity.getUserIdFromJwt();
        } catch (Exception e) {
            log.error("Error getting user account", e);
            return Optional.empty();
        }
    }
}
