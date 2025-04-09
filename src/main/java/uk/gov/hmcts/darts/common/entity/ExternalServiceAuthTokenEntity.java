package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;
import uk.gov.hmcts.darts.util.DataUtil;

import java.time.OffsetDateTime;

@Entity
@Table(name = "external_service_auth_token")
@Getter
@Setter
public class ExternalServiceAuthTokenEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "esa_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "esa_gen")
    @SequenceGenerator(name = "esa_gen", sequenceName = "esa_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "external_service_userid")
    private String externalServiceUserid;

    @Column(name = "token_type")
    private Integer tokenType;

    @Column(name = "token")
    private String token;

    @Column(name = "expiry_ts")
    private OffsetDateTime expiryTimestamp;

}
