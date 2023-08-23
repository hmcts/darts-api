package uk.gov.hmcts.darts.audit.controller.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.darts.audit.model.SearchResult;
import uk.gov.hmcts.darts.common.entity.AuditEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface AuditDtoMapper {

    List<SearchResult> mapToSearchResult(List<AuditEntity> auditEntities);

    default String map(OffsetDateTime value) {
        return value.toString();
    }

    @Mappings({
        @Mapping(source = "createdDateTime", target = "createdAt")
    })
    SearchResult map(AuditEntity auditEntity);

}
