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
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.NaturalId;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.util.UUID;

@Entity
@Table(name = "transient_object_directory")
@Data
@EqualsAndHashCode(callSuper = true)
public class TransientObjectDirectoryEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "tod_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tod_gen")
    @SequenceGenerator(name = "tod_gen", sequenceName = "tod_seq", allocationSize = 1)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mer_id", foreignKey = @ForeignKey(name = "tod_media_request_fk"), nullable = false)
    private MediaRequestEntity mediaRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ods_id", foreignKey = @ForeignKey(name = "tod_object_directory_status_fk"), nullable = false)
    private ObjectDirectoryStatusEntity status;

    @NaturalId
    @Column(name = "external_location", unique = true, nullable = false)
    private UUID externalLocation;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "transfer_attempts")
    private Integer transferAttempts;

}
