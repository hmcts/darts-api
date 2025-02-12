package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotNull;

@Entity
@Table(name = "hearing_media_ae")
@Getter
@Setter
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
@IdClass(HearingToMediaEntity.class)
public class HearingToMediaEntity {
    @ManyToOne
    @JoinColumn(name = "med_id")
    @NotNull
    @Id
    private MediaEntity media;

    @ManyToOne
    @JoinColumn(name = "hea_id")
    @NotNull
    @Id
    private HearingEntity hearing;
}
