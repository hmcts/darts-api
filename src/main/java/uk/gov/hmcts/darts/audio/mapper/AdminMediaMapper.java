package uk.gov.hmcts.darts.audio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.audio.model.AdminActionResponse;
import uk.gov.hmcts.darts.audio.model.AdminMediaCourthouseResponse;
import uk.gov.hmcts.darts.audio.model.AdminMediaCourtroomResponse;
import uk.gov.hmcts.darts.audio.model.AdminMediaHearingResponseItem;
import uk.gov.hmcts.darts.audio.model.AdminMediaResponse;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;

import java.util.List;

@Mapper(componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AdminMediaMapper {

    @Mappings({
        @Mapping(target = "id", source = "id"),
        @Mapping(target = "startAt", source = "start"),
        @Mapping(target = "endAt", source = "end"),
        @Mapping(target = "channel", source = "channel"),
        @Mapping(target = "totalChannels", source = "totalChannels"),
        @Mapping(target = "mediaType", source = "mediaType"),
        @Mapping(target = "mediaFormat", source = "mediaFormat"),
        @Mapping(target = "fileSizeBytes", source = "fileSize"),
        @Mapping(target = "filename", source = "mediaFile"),
        @Mapping(target = "mediaObjectId", source = "legacyObjectId"),
        @Mapping(target = "contentObjectId", source = "contentObjectId"),
        @Mapping(target = "clipId", source = "clipId"),
        @Mapping(target = "checksum", source = "checksum"),
        @Mapping(target = "mediaStatus", source = "mediaStatus"),
        @Mapping(target = "isHidden", source = "hidden"),
        @Mapping(target = "isDeleted", source = "deleted"),
        @Mapping(target = "adminAction", source = "adminActionReasons"),
        @Mapping(target = "version", source = "legacyVersionLabel"),
        @Mapping(target = "chronicleId", source = "chronicleId"),
        @Mapping(target = "antecedentId", source = "antecedentId"),
        @Mapping(target = "retainUntil", source = "retainUntilTs"),
        @Mapping(target = "createdAt", source = "createdDateTime"),
        @Mapping(target = "createdById", source = "createdBy.id"),
        @Mapping(target = "lastModifiedAt", source = "lastModifiedDateTime"),
        @Mapping(target = "lastModifiedById", source = "lastModifiedBy.id"),
        @Mapping(target = "courthouse", source = "courtroom.courthouse"),
        @Mapping(target = "courtroom", source = "courtroom"),
        @Mapping(target = "hearings", source = "hearingList")
    })
    AdminMediaResponse toApiModel(MediaEntity mediaEntity);

    @Mappings({
        @Mapping(target = "id", source = "id"),
        @Mapping(target = "displayName", source = "displayName")
    })
    AdminMediaCourthouseResponse toApiModel(CourthouseEntity courthouseEntity);

    @Mappings({
        @Mapping(target = "id", source = "id"),
        @Mapping(target = "name", source = "name")
    })
    AdminMediaCourtroomResponse toApiModel(CourtroomEntity courtroomEntity);

    @Mappings({
        @Mapping(target = "id", source = "id"),
        @Mapping(target = "reasonId", source = "objectHiddenReason.id"),
        @Mapping(target = "hiddenById", source = "hiddenBy.id"),
        @Mapping(target = "hiddenAt", source = "hiddenDateTime"),
        @Mapping(target = "isMarkedForManualDeletion", source = "markedForManualDeletion"),
        @Mapping(target = "markedForManualDeletionById", source = "markedForManualDelBy.id"),
        @Mapping(target = "markedForManualDeletionAt", source = "markedForManualDelDateTime"),
        @Mapping(target = "ticketReference", source = "ticketReference"),
        @Mapping(target = "comments", source = "comments")
    })
    AdminActionResponse toApiModel(ObjectAdminActionEntity objectAdminActionEntity);

    @Mappings({
        @Mapping(target = "id", source = "id"),
        @Mapping(target = "hearingDate", source = "hearingDate"),
        @Mapping(target = "caseId", source = "courtCase.id")
    })
    AdminMediaHearingResponseItem toApiModel(HearingEntity hearingEntity);

    default AdminActionResponse toApiModel(List<ObjectAdminActionEntity> objectAdminActionEntities) {
        if (objectAdminActionEntities == null || objectAdminActionEntities.isEmpty()) {
            return null;
        }
        return toApiModel(objectAdminActionEntities.get(0));
    }

}
