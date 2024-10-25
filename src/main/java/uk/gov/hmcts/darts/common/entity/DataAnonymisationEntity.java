package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

import java.time.OffsetDateTime;

@Entity
@Table(name = "data_anonymisation")
@Getter
@Setter
@EqualsAndHashCode
public class DataAnonymisationEntity {

    @Id
    @Column(name = "dan_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dan_gen")
    @SequenceGenerator(name = "dan_gen", sequenceName = "dan_seq", allocationSize = 1)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eve_id")
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trc_id")
    private TranscriptionCommentEntity transcriptionComment;

    @Column(name = "is_manual_request")
    private Boolean isManualRequest;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private UserAccountEntity requestedBy;

    @Column(name = "requested_ts")
    private OffsetDateTime requestedTs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private UserAccountEntity approvedBy;

    @Column(name = "approved_ts")
    private OffsetDateTime approvedTs;

}
