package uk.gov.hmcts.darts.audio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.audio.model.AdminMediaCourtroomResponse;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;

@Mapper(componentModel = "spring",
    uses = ObjectActionMapper.class,
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
@FunctionalInterface
public interface CourtroomMapper {

    @Mappings({
        @Mapping(target = "id", source = "id"),
        @Mapping(target = "name", source = "name")
    })
    AdminMediaCourtroomResponse toApiModel(CourtroomEntity courtroomEntity);

}
