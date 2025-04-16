package uk.gov.hmcts.darts.common.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.model.HiddenReason;

import java.util.List;

@Mapper(componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.ERROR,
    unmappedTargetPolicy = ReportingPolicy.ERROR)
@FunctionalInterface
public interface HiddenReasonMapper {

    List<HiddenReason> mapToApiModel(List<ObjectHiddenReasonEntity> hiddenReasonEntities);

}
