package uk.gov.hmcts.darts.audio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.audio.model.AdminActionResponse;
import uk.gov.hmcts.darts.audio.model.AdminMediaHearingResponseItem;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;

import java.util.List;

@Mapper(componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ObjectActionMapper {

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
