package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "media_type")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class MediaTypeEntity {

    @Id
    @Column(name = "met_id")
    private Integer id;

    @Column(name = "media_type")
    private String mediaType;
}
