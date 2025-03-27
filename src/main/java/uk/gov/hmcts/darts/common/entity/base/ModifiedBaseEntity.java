package uk.gov.hmcts.darts.common.entity.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
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

    @Column(name = "last_modified_by")
    private Integer lastModifiedById;

    @Transient
    @JsonIgnore
    @Getter(AccessLevel.NONE)
    private transient UserAccountEntity lastModifiedByUserOverride;


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
        setLastModifiedById(userAccount == null ? null : userAccount.getId());
    }

    @Override
    public void setLastModifiedById(Integer id) {
        this.lastModifiedById = id;
        //Mark skip user audit as true to prevent audit listener from overriding the lastModifiedBy and lastModifiedDateTime
        this.skipUserAudit = true;
    }

    @Transient
    @JsonIgnore
    private transient boolean skipUserAudit = false;
}
