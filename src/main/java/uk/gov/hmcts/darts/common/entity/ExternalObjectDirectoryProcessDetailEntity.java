package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.darts.common.entity.base.MandatoryCreatedModifiedBaseEntity;

import java.time.OffsetDateTime;

@Entity
@Table(name = "extobjdir_process_detail")
@Data
@EqualsAndHashCode(callSuper = true)
public class ExternalObjectDirectoryProcessDetailEntity extends MandatoryCreatedModifiedBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "epd_gen")
    @SequenceGenerator(name = "epd_gen", sequenceName = "epd_seq", allocationSize = 1)
    @Column(name = "epd_id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eod_id")
    private ExternalObjectDirectoryEntity externalObjectDirectory;

    @Column(name = "event_date_ts")
    private OffsetDateTime eventDateTime;

    @Column(name = "update_retention")
    private boolean updateRetention;

}
