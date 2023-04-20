package uk.gov.hmcts.darts.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

@Entity
@Table(name = "moj_transcription_comment")
@Data
public class TranscriptionComment {

    @Id
    @Column(name = "moj_trc_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moj_tra_id")
    private Transcription theTranscription;

    @Column(name = "r_transcription_comment_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "c_comment", length = 1024)
    private String comment;

    @Column(name = "r_transcription_object_id", length = 16)
    private String legacyTranscriptionObjectId;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version_label")
    private Short version;

}
