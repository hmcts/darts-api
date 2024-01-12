package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.util.UUID;

@Entity
@Table(name = "transient_object_directory")
@Getter
@Setter
public class TransientObjectDirectoryEntity extends CreatedModifiedBaseEntity implements ObjectDirectory {

    @Id
    @Column(name = "tod_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tod_gen")
    @SequenceGenerator(name = "tod_gen", sequenceName = "tod_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trm_id", foreignKey = @ForeignKey(name = "tod_transformed_media_fk"), nullable = false)
    private TransformedMediaEntity transformedMedia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ors_id", foreignKey = @ForeignKey(name = "tod_object_record_status_fk"), nullable = false)
    private ObjectRecordStatusEntity status;

    @NaturalId
    @Column(name = "external_location", unique = true, nullable = false)
    private UUID externalLocation;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "transfer_attempts")
    private Integer transferAttempts;

    @Override
    public int getStatusId() {
        return getStatus().getId();
    }

    @Override
    public UUID getLocation() {
        return externalLocation;
    }

}
