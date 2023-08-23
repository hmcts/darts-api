package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.common.entity.base.CreatedModifiedBaseEntity;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

@Entity
@Table(name = "courthouse")
@Getter
@Setter
public class CourthouseEntity extends CreatedModifiedBaseEntity {

    @Id
    @Column(name = "cth_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cth_gen")
    @SequenceGenerator(name = "cth_gen", sequenceName = "cth_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "courthouse_code", unique = true)
    private Integer code;

    @Column(name = "courthouse_name", unique = true)
    private String courthouseName;

    @OneToMany(mappedBy = "courthouse", cascade = {PERSIST, MERGE})
    private List<CourtroomEntity> courtrooms;

    public void addCourtRoom(CourtroomEntity courtroom) {
        if (courtrooms == null) {
            courtrooms = new ArrayList<>();
        }

        courtrooms.add(courtroom);
    }

}
