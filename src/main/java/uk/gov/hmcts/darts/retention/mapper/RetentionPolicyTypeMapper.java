package uk.gov.hmcts.darts.retention.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.retentions.model.AdminPostRetentionRequest;
import uk.gov.hmcts.darts.retentions.model.RetentionPolicyType;

import java.util.List;

@Mapper(componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RetentionPolicyTypeMapper {

    @Mappings({
        @Mapping(source = "policyName", target = "name"),
        @Mapping(source = "policyStart", target = "policyStartAt"),
        @Mapping(source = "policyEnd", target = "policyEndAt"),
    })
    RetentionPolicyType mapToModel(RetentionPolicyTypeEntity entity);

    List<RetentionPolicyType> mapToModelList(List<RetentionPolicyTypeEntity> entities);

    @Mappings({
        @Mapping(source = "name", target = "policyName"),
        @Mapping(source = "policyStartAt", target = "policyStart")
    })
    RetentionPolicyTypeEntity mapToEntity(AdminPostRetentionRequest model);

}
