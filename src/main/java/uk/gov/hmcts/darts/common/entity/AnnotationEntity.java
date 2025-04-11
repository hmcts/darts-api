package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Entity
@Table(name = "annotation")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class AnnotationEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "ann_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ann_gen")
    @SequenceGenerator(name = "ann_gen", sequenceName = "ann_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "annotation_text")
    private String text;

    @Column(name = "annotation_ts")
    private OffsetDateTime timestamp;

    @Column(name = "annotation_object_id")
    private String legacyObjectId;

    @Column(name = "version_label")
    private String legacyVersionLabel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_owner", nullable = false)
    private UserAccountEntity currentOwner;

    @Column(name = "is_deleted")
    private boolean deleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private UserAccountEntity deletedBy;

    @Column(name = "deleted_ts")
    private OffsetDateTime deletedTimestamp;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = AnnotationDocumentEntity_.ANNOTATION)
    private List<AnnotationDocumentEntity> annotationDocuments = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "hearing_annotation_ae",
        joinColumns = {@JoinColumn(name = "ann_id")},
        inverseJoinColumns = {@JoinColumn(name = "hea_id")})
    private Set<HearingEntity> hearings = new HashSet<>();


    public void addHearing(HearingEntity hearingEntity) {
        if (hearingEntity == null) {
            return;
        }
        hearings.add(hearingEntity);
    }

    /**
     * This method was added to simplify the switch from List to Set on HearingEntity in which existing code uses .getFirst()
     * This switch was needed to prevent data integirty issues when inserting/deleting values.
     * As when using a list spring will first delete all values on the mapping table. Then reinsert only the new ones.
     * Where as using a Set it will only add the new values and remove the old ones.
     * A tech debt ticket has be raised to refactor all the code that uses this method, to ensure it uses a many to many safe equivelent
     *
     * @return the first hearing entity found within the set
     * @deprecated because this is not many to many safe. Implementation should account for multiple hearings
     */
    @Deprecated
    public HearingEntity getHearingEntity() {
        return this.getHearings().stream()
            .sorted(Comparator.comparing(HearingEntity::getCreatedDateTime)
                        .thenComparing(HearingEntity::getId))
            .findFirst()
            .orElseThrow(NoSuchElementException::new);
    }

    public boolean isOwnedBy(UserAccountEntity userAccount) {
        return userAccount.getId().equals(currentOwner.getId());
    }
}
