package uk.gov.hmcts.reform.darts.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "moj_courthouse")
@Data
public class Courthouse {

    @Id
    @Column(name = "moj_crt_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "r_courthouse_object_id", length = 16)
    private String legacyObjectId;

    @Column(name = "c_code", length = 32)
    private String code;

    @Column(name = "c_id", length = 32)
    private String idString;

    @Column(name = "c_alias_set_id", length = 16)
    private String aliasSetIdString;

    @Column(name = "r_version_label", length = 32)
    private String legacyVersionLabel;

    @Column(name = "i_superseded")
    private Boolean superseded;

    @Version
    @Column(name = "i_version_label")
    private Short version;

    @OneToMany(mappedBy = "theCourthouse", fetch = FetchType.EAGER)
    private Set<Case> theCases = new HashSet<>();

    @OneToMany(mappedBy = "theCourthouse", fetch = FetchType.EAGER)
    private Set<DailyList> theDailyLists = new HashSet<>();
}
