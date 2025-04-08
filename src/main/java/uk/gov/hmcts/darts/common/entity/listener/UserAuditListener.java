package uk.gov.hmcts.darts.common.entity.listener;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.base.CreatedBy;
import uk.gov.hmcts.darts.common.entity.base.LastModifiedBy;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
public class UserAuditListener {

    private final Clock clock;
    private final UserIdentity userIdentity;


    @Autowired
    public UserAuditListener(Clock clock, @Lazy UserIdentity userIdentity) {
        this.clock = clock;
        this.userIdentity = userIdentity;
    }


    @PrePersist
    void beforeSave(Object object) {
        updateCreatedBy(object);
        updateModifiedBy(object);
    }

    @PreUpdate
    void beforeUpdate(Object object) {
        updateModifiedBy(object);
    }

    @PostLoad
    void postLoad(Object object) {
        if (object instanceof CreatedBy entity) {
            entity.setSkipUserAudit(false);
        }
        if (object instanceof LastModifiedBy entity) {
            entity.setSkipUserAudit(false);
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

    boolean isUserAccountPresent() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    void updateCreatedBy(Object object) {
        if (object instanceof CreatedBy entity) {
            if (entity.isSkipUserAudit() || entity.getCreatedById() != null) {
                return;
            }
            Optional<Integer> userAccountOpt = getUserAccount();
            if (userAccountOpt.isEmpty()) {
                return;
            }
            Integer userAccount = userAccountOpt.get();
            entity.setCreatedById(userAccount);
            entity.setCreatedDateTime(OffsetDateTime.now(clock));
        }
    }


    void updateModifiedBy(Object object) {
        if (object instanceof LastModifiedBy entity) {
            if (entity.isSkipUserAudit()) {
                return;
            }

            Optional<Integer> userAccountOpt = getUserAccount();
            if (userAccountOpt.isEmpty()) {
                return;
            }
            Integer userAccount = userAccountOpt.get();
            entity.setLastModifiedDateTime(OffsetDateTime.now(clock));
            entity.setLastModifiedById(userAccount);
        }
    }
}
