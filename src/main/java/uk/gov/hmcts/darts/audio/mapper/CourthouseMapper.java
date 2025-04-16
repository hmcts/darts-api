package uk.gov.hmcts.darts.audio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.audio.model.AdminMediaCourthouseResponse;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

@Mapper(componentModel = "spring",
    uses = ObjectActionMapper.class,
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
@FunctionalInterface
public interface CourthouseMapper {

    @Mappings({
        @Mapping(target = "id", source = "id"),
        @Mapping(target = "displayName", source = "displayName")
    })
    AdminMediaCourthouseResponse toApiModel(CourthouseEntity courthouseEntity);

}
