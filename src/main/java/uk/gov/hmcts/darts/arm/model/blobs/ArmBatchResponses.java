package uk.gov.hmcts.darts.arm.model.blobs;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseCreateRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseInvalidLineRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecord;
import uk.gov.hmcts.darts.arm.util.files.CreateRecordFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.InvalidLineFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ArmBatchResponses {

    private Map<Long, ArmResponseBatchData> armBatchResponseMap = new HashMap<>();

    public void addResponseBatchData(Long externalObjectDirectoryId,
                                     ArmResponseCreateRecord armResponseCreateRecord,
                                     CreateRecordFilenameProcessor createRecordFilenameProcessor) {
        createArmBatchResponseIfNotExists(externalObjectDirectoryId);
        armBatchResponseMap.get(externalObjectDirectoryId).setArmResponseCreateRecord(armResponseCreateRecord);
        armBatchResponseMap.get(externalObjectDirectoryId).setCreateRecordFilenameProcessor(createRecordFilenameProcessor);
    }


    public void addResponseBatchData(Long externalObjectDirectoryId,
                                     ArmResponseInvalidLineRecord armResponseInvalidLineRecord,
                                     InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor) {
        createArmBatchResponseIfNotExists(externalObjectDirectoryId);
        // Adds the invalid line record and invalid line processor to the lists at the same time
        armBatchResponseMap.get(externalObjectDirectoryId).getArmResponseInvalidLineRecords().add(armResponseInvalidLineRecord);
        armBatchResponseMap.get(externalObjectDirectoryId).getInvalidLineFileFilenameProcessors().add(invalidLineFileFilenameProcessor);
    }

    public void addResponseBatchData(Long externalObjectDirectoryId,
                                     ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                     UploadFileFilenameProcessor uploadFileFilenameProcessor) {
        createArmBatchResponseIfNotExists(externalObjectDirectoryId);
        armBatchResponseMap.get(externalObjectDirectoryId).setArmResponseUploadFileRecord(armResponseUploadFileRecord);
        armBatchResponseMap.get(externalObjectDirectoryId).setUploadFileFilenameProcessor(uploadFileFilenameProcessor);
    }

    private void createArmBatchResponseIfNotExists(Long externalObjectDirectoryId) {
        if (!armBatchResponseMap.containsKey(externalObjectDirectoryId)) {
            ArmResponseBatchData armResponseBatchData = ArmResponseBatchData.builder()
                .externalObjectDirectoryId(externalObjectDirectoryId)
                .build();
            armBatchResponseMap.put(externalObjectDirectoryId, armResponseBatchData);
        }
    }
}
