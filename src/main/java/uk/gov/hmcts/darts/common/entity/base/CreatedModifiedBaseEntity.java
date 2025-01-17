package uk.gov.hmcts.darts.common.entity.base;

import jakarta.persistence.Column;
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

import java.time.OffsetDateTime;

@MappedSuperclass
@Getter
@Setter
public class CreatedModifiedBaseEntity extends CreatedBaseEntity
    implements LastModifiedBy {

    @UpdateTimestamp
    @Column(name = "last_modified_ts")
    private OffsetDateTime lastModifiedDateTime;

    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by", insertable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UserAccountEntity lastModifiedBy;

    @Column(name = "last_modified_by")
    private Integer lastModifiedById;

    @Override
    public void setLastModifiedBy(UserAccountEntity userAccount) {
        this.lastModifiedBy = userAccount;
        this.lastModifiedById = userAccount == null ? null : userAccount.getId();
        //Mark skip user audit as true to prevent audit listener from overriding the lastModifiedBy and lastModifiedDateTime
        this.skipUserAudit = true;
    }
}
