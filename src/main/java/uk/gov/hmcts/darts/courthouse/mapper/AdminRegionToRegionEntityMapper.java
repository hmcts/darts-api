package uk.gov.hmcts.darts.courthouse.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.courthouse.model.AdminRegion;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface AdminRegionToRegionEntityMapper {

    @Mapping(source = "regionName", target = "name")
    AdminRegion mapFromEntitiesToAdminRegions(RegionEntity regionEntity);

    List<AdminRegion> mapFromEntityToAdminRegion(List<RegionEntity> regionEntity);

}
