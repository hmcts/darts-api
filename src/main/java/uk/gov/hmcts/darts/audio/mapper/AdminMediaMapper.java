package uk.gov.hmcts.darts.audio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.audio.model.AdminMediaResponse;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

@Mapper(componentModel = "spring",
    uses = {ObjectActionMapper.class, CourthouseMapper.class, CourtroomMapper.class, MediaLinkedCaseMapper.class},
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
        @Mapping(target = "adminAction", source = "objectAdminActions"),
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
        @Mapping(target = "hearings", source = "hearingList"),
        @Mapping(target = "cases", source = "mediaLinkedCaseList"),
        @Mapping(target = "isCurrent", source = "isCurrent")
    })
    AdminMediaResponse toApiModel(MediaEntity mediaEntity);

}
