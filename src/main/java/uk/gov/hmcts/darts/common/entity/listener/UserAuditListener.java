package uk.gov.hmcts.darts.common.entity.listener;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedBy;
import uk.gov.hmcts.darts.common.entity.base.LastModifiedBy;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class UserAuditListener {

    @Lazy
    @Autowired
    private UserIdentity userIdentity;

    @Autowired
    private Clock clock;


    @PrePersist
    void beforeSave(Object object) {
        log.debug("Before save: {}", object.getClass().getSimpleName());
        Optional<UserAccountEntity> userAccountOpt = getUserAccount();
        if (userAccountOpt.isEmpty()) {
            log.debug("Skipping audit as user account not found");
            return;
        }
        UserAccountEntity userAccount = userAccountOpt.get();
        updateCreatedBy(object, userAccount);
        updateModifiedBy(object, userAccount);
    }

    @PreUpdate
    void beforeUpdate(Object object) {
        log.debug("Before update: {}", object.getClass().getSimpleName());
        Optional<UserAccountEntity> userAccountOpt = getUserAccount();
        if (userAccountOpt.isEmpty()) {
            log.debug("Skipping audit as user account not found");
            return;
        }
        UserAccountEntity userAccount = userAccountOpt.get();
        updateModifiedBy(object, userAccount);
    }

    Optional<UserAccountEntity> getUserAccount() {
        try {
            return Optional.ofNullable(userIdentity.getUserAccount());
        } catch (Exception e) {
            log.error("Error getting user account", e);
            return Optional.empty();
        }
    }


    void updateCreatedBy(Object object, UserAccountEntity userAccount) {
        if (object instanceof CreatedBy entity) {
            if (entity.isSkipUserAudit() || entity.getCreatedBy() != null) {
                log.debug("Skipping audit as isSkipUserAudit is set or createdBy is already set");
                return;
            }
            entity.setCreatedBy(userAccount);
            entity.setCreatedDateTime(OffsetDateTime.now(clock));
        }
    }

    void updateModifiedBy(Object object, UserAccountEntity userAccount) {
        if (object instanceof LastModifiedBy entity) {
            if (entity.isSkipUserAudit()) {
                log.debug("Skipping audit as isSkipUserAudit is set");
                return;
            }
            entity.setLastModifiedBy(userAccount);
            entity.setLastModifiedDateTime(OffsetDateTime.now(clock));
        }
    }
}
