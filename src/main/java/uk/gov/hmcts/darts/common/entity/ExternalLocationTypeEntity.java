package uk.gov.hmcts.darts.common.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import uk.gov.hmcts.darts.util.DataUtil;

@Entity
@Table(name = "external_location_type")
@Getter
@Setter
@ToString
// prevents "hibernateLazyInitializer" property to end-up in json when serialising
@JsonIgnoreProperties({"hibernateLazyInitializer"})
public class ExternalLocationTypeEntity {

    @Id
    @Column(name = "elt_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "elt_gen")
    @SequenceGenerator(name = "elt_gen", sequenceName = "elt_seq", allocationSize = DataUtil.DEFAULT_SEQUENCE_ALLOCATION_SIZE)
    private Integer id;

    @Column(name = "elt_description")
    private String description;

}
