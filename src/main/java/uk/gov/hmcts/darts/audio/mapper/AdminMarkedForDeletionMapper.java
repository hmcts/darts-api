package uk.gov.hmcts.darts.audio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.audio.model.GetAdminMediasMarkedForDeletionMediaItem;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasMarkedForDeletionItem;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
    uses = {ObjectActionMapper.class, CourthouseMapper.class, CourtroomMapper.class},
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AdminMarkedForDeletionMapper {

    @Mappings({
        @Mapping(target = "mediaId", source = "id"),
        @Mapping(target = "startAt", source = "start"),
        @Mapping(target = "endAt", source = "end"),
        @Mapping(target = "channel", source = "channel"),
        @Mapping(target = "courthouse", source = "courtroom.courthouse"),
        @Mapping(target = "courtroom", source = "courtroom"),
        @Mapping(target = "adminAction", source = "adminActionReasons")
    })
    public PostAdminMediasMarkedForDeletionItem toApiModel(MediaEntity mediaEntity);

    @Mappings({
        @Mapping(target = "versionCount", ignore = true)
    })
    public GetAdminMediasMarkedForDeletionMediaItem toGetAdminMediasMarkedForDeletionMediaItem(MediaEntity mediaEntity);
}
