package uk.gov.hmcts.darts.audit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import uk.gov.hmcts.darts.audit.service.AuditorRevisionListener;

import java.io.Serializable;

@Entity
@Table(name = "revinfo")
@RevisionEntity(AuditorRevisionListener.class)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RevisionInfo implements Serializable {

    private static final String GENERATOR_NAME = "revinfo_revision_gen";

    @Id
    @GeneratedValue(generator = GENERATOR_NAME, strategy = GenerationType.SEQUENCE)
    @SequenceGenerator(name = GENERATOR_NAME, sequenceName = "revinfo_seq",
        allocationSize = 1)
    @RevisionNumber
    @Column(name = "rev")
    private Long revision;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private Long timestamp;


    @Column(name = "audit_user")
    private Integer auditUser;
}
