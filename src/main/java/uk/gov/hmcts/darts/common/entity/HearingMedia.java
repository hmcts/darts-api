package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = HearingMedia.TABLE_NAME)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HearingMedia {

    public static final String HEARING_MEDIA_ID = "moj_hma_id";
    public static final String HEARING_ID = "moj_hea_id";
    public static final String MEDIA_ID = "moj_med_id";
    public static final String TABLE_NAME = "moj_hearing_media_ae";

    @Id
    @Column(name = HEARING_MEDIA_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hearing_media_gen")
    @SequenceGenerator(name = "hearing_media_gen", sequenceName = "hearing_media_seq", allocationSize = 1)
    private Integer hearingMediaId;

    @Column(name = HEARING_ID)
    private Integer hearingId;

    @Column(name = MEDIA_ID)
    private Integer mediaId;

}

