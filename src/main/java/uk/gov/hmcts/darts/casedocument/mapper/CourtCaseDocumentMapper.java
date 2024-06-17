package uk.gov.hmcts.darts.casedocument.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import uk.gov.hmcts.darts.casedocument.template.CourtCaseDocument;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;

@Mapper(componentModel = "spring")
public interface CourtCaseDocumentMapper {

    @Mappings({
//        @Mapping(source = "caseRetentionEntities", target = "caseRetentions"),
        @Mapping(source = "lastModifiedBy.id", target = "lastModifiedBy"),
        @Mapping(source = "createdBy.id", target = "createdBy"),
        @Mapping(source = "deletedBy.id", target = "deletedBy"),
        @Mapping(source = "reportingRestrictions.createdBy.id", target = "reportingRestrictions.createdBy"),
        @Mapping(source = "defendantList", target = "defendants"),
        @Mapping(source = "prosecutorList", target = "prosecutors"),
        @Mapping(source = "defenceList", target = "defences"),
    })
    CourtCaseDocument mapToCourtCaseDocument(CourtCaseEntity courtCase);
}
