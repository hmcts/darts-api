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

@Entity
@Table(name = NodeRegisterEntity.TABLE_NAME)
@Getter
@Setter
public class NodeRegisterEntity {

    public static final String TABLE_NAME = "node_register";
    public static final String NODE_GENERATOR = "nod_gen";
    public static final String NODE_SEQUENCE = "nod_seq";
    public static final String DEVICE_ID = "der_id";
    public static final String COURTROOM_ID = "ctr_id";
    public static final String NODE_ID = "node_id";
    public static final String HOSTNAME = "hostname";
    public static final String IP_ADDRESS = "ip_address";
    public static final String MAC_ADDRESS = "mac_address";
    public static final String DEVICE_TYPE = "device_type";

    @Column(name = DEVICE_ID)
    private Integer deviceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = COURTROOM_ID, nullable = false)
    private CourtroomEntity courtroom;

    @Id
    @Column(name = NODE_ID)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NODE_GENERATOR)
    @SequenceGenerator(name = NODE_GENERATOR, sequenceName = NODE_SEQUENCE, allocationSize = 1)
    private int nodeId;

    @Column(name = HOSTNAME)
    private String hostname;

    @Column(name = IP_ADDRESS)
    private String ipAddress;

    @Column(name = MAC_ADDRESS)
    private String macAddress;

    @Column(name = DEVICE_TYPE)
    private String deviceType;

}
