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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "moj_media")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class MediaEntity extends VersionedEntity {

    @Id
    @Column(name = "moj_med_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "moj_med_gen")
    @SequenceGenerator(name = "moj_med_gen", sequenceName = "moj_med_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moj_ctr_id", foreignKey = @ForeignKey(name = "moj_media_courtroom_fk"))
    private CourtroomEntity courtroom;

    @Column(name = "r_media_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "c_channel")
    private Integer channel;

    @Column(name = "c_total_channels")
    private Integer totalChannels;

    @Column(name = "c_reference_id")
    private String referenceId;

    @Column(name = "c_start")
    private OffsetDateTime start;

    @Column(name = "c_end")
    private OffsetDateTime end;

    @Type(ListArrayType.class)
    @Column(name = "c_case_id")
    private List<String> caseIdList = new ArrayList<>();

    @Type(ListArrayType.class)
    @Column(name = "r_case_object_id")
    private List<String> caseObjectIdList = new ArrayList<>();

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

}
