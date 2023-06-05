package uk.gov.hmcts.darts.courthouse.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.courthouse.model.Courthouse;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthouse;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface CourthouseToCourthouseEntityMapper {
    uk.gov.hmcts.darts.common.entity.Courthouse mapToEntity(Courthouse courthouse);

    ExtendedCourthouse mapFromEntityToExtendedCourthouse(uk.gov.hmcts.darts.common.entity.Courthouse courthouseEntity);

    @Mappings({
        @Mapping(target = "createdDateTime", source = "createdDateTime"),
        @Mapping(target = "lastModifiedDateTime", source = "lastModifiedDateTime")
    })
    List<ExtendedCourthouse> mapFromListEntityToListExtendedCourthouse(List<uk.gov.hmcts.darts.common.entity.Courthouse> courthouses);
}
