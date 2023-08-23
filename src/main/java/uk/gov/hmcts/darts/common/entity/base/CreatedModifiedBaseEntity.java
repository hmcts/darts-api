package uk.gov.hmcts.darts.common.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

@MappedSuperclass
@Getter
@Setter
public class CreatedModifiedBaseEntity {

    @CreationTimestamp
    @Column(name = "created_ts")
    private OffsetDateTime createdDateTime;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {PERSIST, MERGE})
    @JoinColumn(name = "created_by", referencedColumnName = "usr_id")
    private UserAccountEntity createdBy;

    @UpdateTimestamp
    @Column(name = "last_modified_ts")
    private OffsetDateTime lastModifiedDateTime;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {PERSIST, MERGE})
    @JoinColumn(name = "last_modified_by", referencedColumnName = "usr_id")
    private UserAccountEntity lastModifiedBy;
}
