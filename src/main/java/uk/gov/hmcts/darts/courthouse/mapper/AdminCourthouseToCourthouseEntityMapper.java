package uk.gov.hmcts.darts.courthouse.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.courthouse.model.Courthouse;
import uk.gov.hmcts.darts.courthouse.model.ExtendedAdminCourthouse;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface AdminCourthouseToCourthouseEntityMapper {
    CourthouseEntity mapToEntity(Courthouse courthouse);

    ExtendedAdminCourthouse mapFromEntityToExtendedAdminCourthouse(CourthouseEntity courthouseEntity);

    @Mappings({
        @Mapping(target = "createdDateTime", source = "createdDateTime", qualifiedByName = "createdDateTime"),
        @Mapping(target = "lastModifiedDateTime", source = "lastModifiedDateTime", qualifiedByName = "lastModifiedDateTime")
    })
    List<ExtendedAdminCourthouse> mapFromListEntityToListExtendedAdminCourthouse(List<CourthouseEntity> courthouses);

    default String map(OffsetDateTime value) {
        return value.toString();
    }
}
