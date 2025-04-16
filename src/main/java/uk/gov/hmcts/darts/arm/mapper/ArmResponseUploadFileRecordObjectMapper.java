package uk.gov.hmcts.darts.arm.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecordObject;

@Mapper(componentModel = "spring")
@FunctionalInterface
public interface ArmResponseUploadFileRecordObjectMapper {

    @Mapping(target = "input", ignore = true)
    ArmResponseUploadFileRecordObject map(ArmResponseUploadFileRecord armResponseUploadFileRecord);

}
