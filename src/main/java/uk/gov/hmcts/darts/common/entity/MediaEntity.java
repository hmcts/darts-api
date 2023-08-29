package uk.gov.hmcts.darts.common.entity;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "media")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class MediaEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "med_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "med_gen")
    @SequenceGenerator(name = "med_gen", sequenceName = "med_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ctr_id", foreignKey = @ForeignKey(name = "media_courtroom_fk"))
    private CourtroomEntity courtroom;

    @Column(name = "media_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "channel")
    private Integer channel;

    @Column(name = "total_channels")
    private Integer totalChannels;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "start_ts")
    private OffsetDateTime start;

    @Column(name = "end_ts")
    private OffsetDateTime end;

    @Type(ListArrayType.class)
    @Column(name = "case_number")
    private List<String> caseIdList = new ArrayList<>();

    @Column(name = "version_label", length = 32)
    private String legacyVersionLabel;

}
