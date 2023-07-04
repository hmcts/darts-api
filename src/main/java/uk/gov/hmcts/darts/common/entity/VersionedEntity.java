package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
public class VersionedEntity {

    @Version
    @Column(name = "i_version")
    @Getter
    @Setter
    private Integer version;

}
