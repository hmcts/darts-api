package uk.gov.hmcts.darts.audio.mapper;

import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audio.model.GetAdminMediasMarkedForDeletionAdminAction;
import uk.gov.hmcts.darts.audio.model.GetAdminMediasMarkedForDeletionItem;
import uk.gov.hmcts.darts.audio.model.GetAdminMediasMarkedForDeletionMediaItem;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasMarkedForDeletionItem;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.repository.MediaRepository;

import java.util.List;

@Mapper(componentModel = "spring",
    uses = {ObjectActionMapper.class, CourthouseMapper.class, CourtroomMapper.class},
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public abstract class AdminMarkedForDeletionMapper {

    private CourthouseMapper courthouseMapper;
    private CourtroomMapper courtroomMapper;
    private ObjectActionMapper objectActionMapper;
    private MediaRepository mediaRepository;

    @Mappings({
        @Mapping(target = "mediaId", source = "id"),
        @Mapping(target = "startAt", source = "start"),
        @Mapping(target = "endAt", source = "end"),
        @Mapping(target = "channel", source = "channel"),
        @Mapping(target = "courthouse", source = "courtroom.courthouse"),
        @Mapping(target = "courtroom", source = "courtroom"),
        @Mapping(target = "adminAction", source = "adminActionReasons")
    })
    public abstract PostAdminMediasMarkedForDeletionItem toApiModel(MediaEntity mediaEntity);

    @Mappings({
        @Mapping(target = "versionCount", ignore = true)
    })
    public abstract GetAdminMediasMarkedForDeletionMediaItem toGetAdminMediasMarkedForDeletionMediaItem(MediaEntity mediaEntity);

    public GetAdminMediasMarkedForDeletionItem toGetAdminMediasMarkedForDeletionItem(List<ObjectAdminActionEntity> actions) {
        ObjectAdminActionEntity base = actions.get(0);
        List<GetAdminMediasMarkedForDeletionMediaItem> media = actions.stream()
            .map(action -> action.getMedia())
            .map(mediaEntity -> {
                GetAdminMediasMarkedForDeletionMediaItem item = toGetAdminMediasMarkedForDeletionMediaItem(mediaEntity);
                item.setVersionCount(mediaRepository.getVersionCount(mediaEntity.getChronicleId()));
                return item;
            })
            .toList();
        GetAdminMediasMarkedForDeletionItem item = new GetAdminMediasMarkedForDeletionItem();
        item.setMedia(media);
        item.setStartAt(base.getMedia().getStart());
        item.setEndAt(base.getMedia().getEnd());
        item.setCourthouse(courthouseMapper.toApiModel(base.getMedia().getCourtroom().getCourthouse()));
        item.setCourtroom(courtroomMapper.toApiModel(base.getMedia().getCourtroom()));
        GetAdminMediasMarkedForDeletionAdminAction adminAction = objectActionMapper.toGetAdminMediasMarkedForDeletionAdminAction(base);
        adminAction.setComments(actions.stream().map(ObjectAdminActionEntity::getComments).toList());
        item.setAdminAction(adminAction);
        return item;
    }
}
