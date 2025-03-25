package uk.gov.hmcts.darts.common.entity.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.NotAudited;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.listener.UserAuditListener;

import java.time.OffsetDateTime;

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
        this.lastModifiedBy = userAccount;
        this.lastModifiedById = userAccount == null ? null : userAccount.getId();
        //Mark skip user audit as true to prevent audit listener from overriding the lastModifiedBy and lastModifiedDateTime
        this.skipUserAudit = true;
    }

    @Transient
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private transient UserAccountEntity tempLastModifiedBy;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        // Store the lastModifiedBy in a temporary variable to be used in postPersist
        // This is required to prevent the incorrect log messages of
        // entity was modified but it wont be updated in the database becuase it is marked as updatable = false
        tempLastModifiedBy = lastModifiedBy;
        lastModifiedBy = null;
    }

    @PostPersist
    @PostUpdate
    public void postPersist() {
        lastModifiedBy = tempLastModifiedBy;
        tempLastModifiedBy = null;
    }
}
