package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import uk.gov.hmcts.darts.common.entity.base.CreatedBaseEntity;

@Entity
@Table(name = NodeRegisterEntity.TABLE_NAME)
@Getter
@Setter
@Audited
@AuditTable("node_register_aud")
public class NodeRegisterEntity extends CreatedBaseEntity {

    public static final String TABLE_NAME = "node_register";
    public static final String NODE_GENERATOR = "nod_gen";
    public static final String NODE_SEQUENCE = "nod_seq";
    public static final String COURTROOM_ID = "ctr_id";
    public static final String NODE_ID = "node_id";
    public static final String HOSTNAME = "hostname";
    public static final String IP_ADDRESS = "ip_address";
    public static final String MAC_ADDRESS = "mac_address";
    public static final String NODE_TYPE = "node_type";

    @NotAudited
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = COURTROOM_ID, nullable = false)
    private CourtroomEntity courtroom;

    @Id
    @Column(name = NODE_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NODE_GENERATOR)
    @SequenceGenerator(name = NODE_GENERATOR, sequenceName = NODE_SEQUENCE, allocationSize = 1)
    private int nodeId;

    @Column(name = HOSTNAME, nullable = false)
    private String hostname;

    @Column(name = IP_ADDRESS, nullable = false)
    private String ipAddress;

    @Column(name = MAC_ADDRESS, nullable = false)
    private String macAddress;

    @Column(name = NODE_TYPE)
    private String nodeType;

}
