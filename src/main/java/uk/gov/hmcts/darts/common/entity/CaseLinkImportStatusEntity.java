package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "case_link_import_status")
@Getter
@NoArgsConstructor
@Setter
public class CaseLinkImportStatusEntity {

    @Id
    @Column(name = "cis_id", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "status_type", nullable = false, updatable = false)
    private String statusType;

    @Column(name = "display_name", nullable = false, updatable = false)
    private String displayName;

    public CaseLinkImportStatusEntity(Integer id) {
        this.id = id;
    }
}
