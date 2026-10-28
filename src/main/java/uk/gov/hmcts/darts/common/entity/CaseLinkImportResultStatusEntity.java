package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "case_link_import_result_status")
@Getter
@NoArgsConstructor
@Setter
public class CaseLinkImportResultStatusEntity {

    @Id
    @Column(name = "crs_id", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "status_type", nullable = false, updatable = false)
    private String statusType;

    @Column(name = "display_name", nullable = false, updatable = false)
    private String displayName;

    public CaseLinkImportResultStatusEntity(Integer id) {
        this.id = id;
    }
}
