package uk.gov.hmcts.darts.courthouse.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.courthouse.model.CourthousePost;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthouse;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthousePost;

import java.time.OffsetDateTime;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface CourthouseToCourthouseEntityMapper {

    CourthouseEntity mapToEntity(CourthousePost courthousePost);

    ExtendedCourthouse mapFromEntityToExtendedCourthouse(CourthouseEntity courthouseEntity);

    ExtendedCourthousePost mapToExtendedCourthousePost(CourthouseEntity courthouseEntity);

    default String map(OffsetDateTime value) {
        return value.toString();
    }

}
