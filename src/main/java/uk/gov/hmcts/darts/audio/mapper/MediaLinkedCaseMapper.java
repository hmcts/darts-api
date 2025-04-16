package uk.gov.hmcts.darts.audio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.openapitools.jackson.nullable.JsonNullable;
import uk.gov.hmcts.darts.audio.model.AdminMediaCaseResponseItem;
import uk.gov.hmcts.darts.audio.model.AdminMediaCaseResponseItemCourthouse;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType;

@Mapper(componentModel = "spring",
    uses = {CourthouseMapper.class},
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
@FunctionalInterface
public interface MediaLinkedCaseMapper {


    @Mappings({
        @Mapping(target = "id", source = ".", qualifiedByName = "mapId"),
        @Mapping(target = "caseNumber", source = ".", qualifiedByName = "getCaseNumber"),
        @Mapping(target = "source", source = "source", qualifiedByName = "mapSource"),
        @Mapping(target = "courthouse", source = ".", qualifiedByName = "mapCourthouse")
    })
    AdminMediaCaseResponseItem toApiModel(MediaLinkedCaseEntity mediaLinkedCase);

    @Named("mapId")
    default JsonNullable<Integer> mapId(MediaLinkedCaseEntity mediaLinkedCase) {
        if (mediaLinkedCase.getCourtCase() != null) {
            return JsonNullable.of(mediaLinkedCase.getCourtCase().getId());
        }
        return JsonNullable.of(null);
    }

    @Named("getCaseNumber")
    default String getCaseNumber(MediaLinkedCaseEntity mediaLinkedCase) {
        if (mediaLinkedCase.getCourtCase() != null) {
            return mediaLinkedCase.getCourtCase().getCaseNumber();
        }
        return mediaLinkedCase.getCaseNumber();
    }

    @Named("mapSource")
    default AdminMediaCaseResponseItem.SourceEnum mapSource(MediaLinkedCaseSourceType source) {
        if (source == null) {
            return null;
        }
        return switch (source) {
            case LEGACY -> AdminMediaCaseResponseItem.SourceEnum.LEGACY;
            case ADD_AUDIO_METADATA -> AdminMediaCaseResponseItem.SourceEnum.ADD_AUDIO_METADATA;
            case ADD_AUDIO_EVENT_LINKING -> AdminMediaCaseResponseItem.SourceEnum.ADD_AUDIO_EVENT_LINKING;
            case AUDIO_LINKING_TASK -> AdminMediaCaseResponseItem.SourceEnum.AUDIO_LINKING_TASK;
        };
    }

    @Named("mapCourthouse")
    default AdminMediaCaseResponseItemCourthouse mapCourthouse(MediaLinkedCaseEntity mediaLinkedCase) {
        AdminMediaCaseResponseItemCourthouse courthouse = new AdminMediaCaseResponseItemCourthouse();

        if (mediaLinkedCase.getCourtCase() != null && mediaLinkedCase.getCourtCase().getCourthouse() != null) {
            courthouse.setId(mediaLinkedCase.getCourtCase().getCourthouse().getId());
            courthouse.setDisplayName(mediaLinkedCase.getCourtCase().getCourthouse().getDisplayName());
        } else {
            courthouse.setId(null);
            courthouse.setDisplayName(mediaLinkedCase.getCourthouseName());
        }

        return courthouse;
    }
}