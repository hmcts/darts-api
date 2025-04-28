package uk.gov.hmcts.darts.casedocument.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class MediaCaseDocument extends CreatedModifiedCaseDocument {

    private final Long id;
    private final String legacyObjectId;
    private final Integer channel;
    private final Integer totalChannels;
    private final OffsetDateTime start;
    private final OffsetDateTime end;
    private final String legacyVersionLabel;
    private final String mediaFile;
    private final String mediaFormat;
    private final Long fileSize;
    private final String checksum;
    private final Character mediaType;
    private final String contentObjectId;
    private final String clipId;
    private final String chronicleId;
    private final String antecedentId;
    private final boolean hidden;
    private final boolean deleted;
    private final Integer deletedBy;
    private final OffsetDateTime deletedTimestamp;
    private final String mediaStatus;
    private final OffsetDateTime retainUntilTs;
    private final List<ObjectAdminActionCaseDocument> adminActionReasons;
    private final List<ExternalObjectDirectoryCaseDocument> externalObjectDirectories;
}
