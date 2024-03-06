package uk.gov.hmcts.darts.courthouse.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.courthouse.model.CourthousePost;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthousePost;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface CourthousePostToCourthouseEntityMapper {
    CourthouseEntity mapToEntity(CourthousePost courthousePost);

    CourthousePost mapFromEntityToCourthousePost(CourthouseEntity courthouseEntity);

    ExtendedCourthousePost mapFromEntityToExtendedCourthousePost(CourthousePost courthousePost);
}
