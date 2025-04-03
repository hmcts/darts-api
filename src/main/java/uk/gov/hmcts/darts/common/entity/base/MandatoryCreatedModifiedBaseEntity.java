package uk.gov.hmcts.darts.common.entity.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.NotAudited;
import org.springframework.data.annotation.Transient;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.listener.UserAuditListener;

import java.time.OffsetDateTime;
import java.util.Optional;

@MappedSuperclass
@Getter
@Setter
@EntityListeners(UserAuditListener.class)
public class MandatoryCreatedModifiedBaseEntity extends MandatoryCreatedBaseEntity implements LastModifiedBy {

    @UpdateTimestamp
    @Column(name = "last_modified_ts", nullable = false)
    private OffsetDateTime lastModifiedDateTime;

    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by", insertable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UserAccountEntity lastModifiedBy;

    @Column(name = "last_modified_by", nullable = false)
    private Integer lastModifiedById;

    @Override
    public void setLastModifiedBy(UserAccountEntity userAccount) {
        this.lastModifiedByUserOverride = userAccount;
        //Set user override to the new user account. This prevents the incorrect log message (see below) from being set
        //The [lastModifiedBy] property of the [...] entity was modified, but it won't be updated because the property is immutable.
        this.lastModifiedById = userAccount == null ? null : userAccount.getId();
        //Mark skip user audit as true to prevent audit listener from overriding the lastModifiedBy and lastModifiedDateTime
        this.skipUserAudit = true;
    }

    @Transient
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private transient UserAccountEntity lastModifiedByUserOverride;

    public UserAccountEntity getLastModifiedBy() {
        //Get user override if set else return the lastModifiedBy (Prevents the incorrect log message from being set)
        return Optional.ofNullable(lastModifiedByUserOverride).orElse(lastModifiedBy);
    }
}
