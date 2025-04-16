package uk.gov.hmcts.darts.common.entity.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.NotAudited;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.entity.listener.UserAuditListener;

import java.time.OffsetDateTime;

@MappedSuperclass
@Getter
@Setter
@EntityListeners(UserAuditListener.class)
public class CreatedBaseEntity implements CreatedBy {
    @CreationTimestamp
    @Column(name = "created_ts")
    private OffsetDateTime createdDateTime;

    @NotAudited
    @Column(name = "created_by")
    private Integer createdById;

    @Transient
    @JsonIgnore
    protected transient boolean skipUserAudit;

    @Override
    public void setCreatedBy(UserAccountEntity userAccount) {
        setCreatedById(userAccount == null ? null : userAccount.getId());
    }

    @Override
    public void setCreatedById(Integer id) {
        this.createdById = id;
        //Mark skip user audit as true to prevent audit listener from overriding the createdBy and createdDateTime
        this.skipUserAudit = true;
    }
}
