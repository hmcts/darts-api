package uk.gov.hmcts.darts.common.entity.base;

import jakarta.persistence.CascadeType;
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
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "last_modified_by", insertable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UserAccountEntity lastModifiedBy;

    @Column(name = "last_modified_by")
    private Integer lastModifiedById;

    @Override
    public void setLastModifiedBy(UserAccountEntity userAccount) {
        this.lastModifiedBy = userAccount;
        this.lastModifiedById = userAccount == null ? null : userAccount.getId();
        this.skipUserAudit = true;//As this was manualy set we should not override it
    }

}
