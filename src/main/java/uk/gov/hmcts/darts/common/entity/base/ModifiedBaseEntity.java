package uk.gov.hmcts.darts.common.entity.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.listener.UserAuditListener;

import java.time.OffsetDateTime;

@MappedSuperclass
@Getter
@Setter
@EntityListeners(UserAuditListener.class)
public class ModifiedBaseEntity implements LastModifiedBy {

    @UpdateTimestamp
    @Column(name = "last_modified_ts")
    private OffsetDateTime lastModifiedTimestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by", insertable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UserAccountEntity lastModifiedBy;

    @Column(name = "last_modified_by")
    private Integer lastModifiedById;

    @Override
    public void setLastModifiedDateTime(OffsetDateTime lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    @Override
    public OffsetDateTime getLastModifiedDateTime() {
        return this.lastModifiedTimestamp;
    }

    @Override
    public void setLastModifiedBy(UserAccountEntity userAccount) {
        this.lastModifiedBy = userAccount;
        this.lastModifiedById = userAccount == null ? null : userAccount.getId();
        //Mark skip user audit as true to prevent audit listener from overriding the lastModifiedBy and lastModifiedDateTime
        this.skipUserAudit = true;
    }

    @Transient
    @JsonIgnore
    private transient boolean skipUserAudit = false;
}
