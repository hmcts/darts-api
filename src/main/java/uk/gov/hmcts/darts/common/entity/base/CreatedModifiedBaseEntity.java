package uk.gov.hmcts.darts.common.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
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

    @Column(name = "last_modified_by")
    private Integer lastModifiedById;


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
}
