package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_heritage")
@Getter
@Setter
@SuppressWarnings("checkstyle:MemberName")
public class AuditHeritageEntity {
    @Id
    @Column(name = "R_OBJECT_ID")
    private String rObjectId;

    @Column(name = "EVENT_NAME")
    private String eventName;

    @Column(name = "EVENT_SOURCE")
    private String eventSource;

    @Column(name = "R_GEN_SOURCE")
    private Integer rGenSource;

    @Column(name = "USER_NAME")
    private String userName;

    @Column(name = "AUDITED_OBJ_ID")
    private String auditedObjId;

    @Column(name = "TIME_STAMP")
    private OffsetDateTime timeStamp;

    @Column(name = "STRING_1")
    private String string1;

    @Column(name = "STRING_2")
    private String string2;

    @Column(name = "STRING_3")
    private String string3;

    @Column(name = "STRING_4")
    private String string4;

    @Column(name = "STRING_5")
    private String string5;

    @Column(name = "ID_1")
    private String id1;

    @Column(name = "ID_2")
    private String id2;

    @Column(name = "ID_3")
    private String id3;

    @Column(name = "ID_4")
    private String id4;

    @Column(name = "ID_5")
    private String id5;

    @Column(name = "CHRONICLE_ID")
    private String chronicleId;

    @Column(name = "OBJECT_NAME")
    private String objectName;

    @Column(name = "VERSION_LABEL")
    private String versionLabel;

    @Column(name = "OBJECT_TYPE")
    private String objectType;

    @Column(name = "EVENT_DESCRIPTION")
    private String eventDescription;

    @Column(name = "POLICY_ID")
    private String policyId;

    @Column(name = "CURRENT_STATE")
    private String currentState;

    @Column(name = "WORKFLOW_ID")
    private String workflowId;

    @Column(name = "SESSION_ID")
    private String sessionId;

    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "OWNER_NAME")
    private String ownerName;

    @Column(name = "ACL_NAME")
    private String aclName;

    @Column(name = "ACL_DOMAIN")
    private String aclDomain;

    @Column(name = "APPLICATION_CODE")
    private String applicationCode;

    @Column(name = "CONTROLLING_APP")
    private String controllingApp;

    @Column(name = "ATTRIBUTE_LIST")
    private String attributeList;

    @Column(name = "ATTRIBUTE_LIST_ID")
    private String attributeListId;

    @Column(name = "AUDIT_SIGNATURE")
    private String auditSignature;

    @Column(name = "AUDIT_VERSION")
    private Integer auditVersion;

    @Column(name = "HOST_NAME")
    private String hostName;

    @Column(name = "TIME_STAMP_UTC")
    private OffsetDateTime timeStampUtc;

    @Column(name = "I_AUDITED_OBJ_CLASS")
    private Integer iAuditedObjClass;

    @Column(name = "REGISTRY_ID")
    private String registryId;

    @Column(name = "I_IS_ARCHIVED")
    private Integer iIsArchived;

    @Column(name = "AUDITED_OBJ_VSTAMP")
    private Integer auditedObjVstamp;

    @Column(name = "ATTRIBUTE_LIST_OLD")
    private String attributeListOld;

    @Column(name = "I_IS_REPLICA")
    private Integer iIsReplica;

    @Column(name = "I_VSTAMP")
    private Integer iVstamp;

    @Column(name = "ATTRIBUTE_LIST_ASPECT_ID")
    private String attributeListAspectId;

    @Id
    @Column(name = "R_OBJECT_SEQUENCE")
    private Integer rObjectSequence;
}
