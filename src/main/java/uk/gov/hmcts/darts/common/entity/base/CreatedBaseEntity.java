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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private UserAccountEntity createdBy;

    @NotAudited
    @Column(name = "created_by")
    private Integer createdById;

    @Override
    public void setCreatedBy(UserAccountEntity userAccount) {
        this.createdBy = userAccount;
        this.createdById = userAccount == null ? null : createdBy.getId();
        //Mark skip user audit as true to prevent audit listener from overriding the createdBy and createdDateTime
        this.skipUserAudit = true;
    }

    @Transient
    @JsonIgnore
    protected transient boolean skipUserAudit = false;
}
