package uk.gov.hmcts.darts.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity(name = HearingReportingRestrictionsEntity.VIEW_NAME)
@Immutable
@Getter
public class HearingReportingRestrictionsEntity {
    public static final String VIEW_NAME = "hearing_reporting_restrictions";

    @Id
    @Column(name = "id")
    Long id;

    @Column(name = "cas_id")
    Integer caseId;

    @Column(name = "hea_id")
    Integer hearingId;

    @Column(name = "event_type")
    String eventType;

    @Column(name = "event_sub_type")
    String eventSubType;

    @Column(name = "active")
    boolean active;

    @Column(name = "eve_id")
    Integer eventId;

    @Column(name = "ctr_id")
    Integer courtroomId;

    @Column(name = "evh_id")
    Integer eventHandlerId;

    @Column(name = "event_object_id")
    String eventObjectId;

    @Column(name = "event_id")
    Integer mojEventId;

    @Column(name = "event_name")
    String eventName;

    @Column(name = "event_text")
    String eventText;

    @Column(name = "event_ts")
    OffsetDateTime eventDateTime;

    @Column(name = "case_number")
    private List<String> caseNumberList = new ArrayList<>();

    @Column(name = "version_label")
    String versionLabel;

    @Column(name = "message_id")
    String messageId;

    @Column(name = "created_ts")
    OffsetDateTime createdDateTime;

    @Column(name = "created_by")
    Integer createdBy;

    @Column(name = "last_modified_ts")
    OffsetDateTime lastModifiedDateTime;

    @Column(name = "last_modified_by")
    Integer lastModifiedBy;
}
