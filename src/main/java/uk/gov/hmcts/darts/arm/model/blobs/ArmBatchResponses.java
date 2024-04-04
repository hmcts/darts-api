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

    private Map<Integer, ArmResponseBatchData> armBatchResponses = new HashMap<>();

    public void addResponseBatchData(Integer externalObjectDirectoryId,
                                     ArmResponseCreateRecord armResponseCreateRecord,
                                     CreateRecordFilenameProcessor createRecordFilenameProcessor) {
        createArmBatchResponseIfNotExists(externalObjectDirectoryId);
        armBatchResponses.get(externalObjectDirectoryId).setArmResponseCreateRecord(armResponseCreateRecord);
        armBatchResponses.get(externalObjectDirectoryId).setCreateRecordFilenameProcessor(createRecordFilenameProcessor);
    }


    public void addResponseBatchData(Integer externalObjectDirectoryId,
                                     ArmResponseInvalidLineRecord armResponseInvalidLineRecord,
                                     InvalidLineFileFilenameProcessor invalidLineFileFilenameProcessor) {
        createArmBatchResponseIfNotExists(externalObjectDirectoryId);
        armBatchResponses.get(externalObjectDirectoryId).setArmResponseInvalidLineRecord(armResponseInvalidLineRecord);
        armBatchResponses.get(externalObjectDirectoryId).setInvalidLineFileFilenameProcessor(invalidLineFileFilenameProcessor);
    }

    public void addResponseBatchData(Integer externalObjectDirectoryId,
                                     ArmResponseUploadFileRecord armResponseUploadFileRecord,
                                     UploadFileFilenameProcessor uploadFileFilenameProcessor) {
        createArmBatchResponseIfNotExists(externalObjectDirectoryId);
        armBatchResponses.get(externalObjectDirectoryId).setArmResponseUploadFileRecord(armResponseUploadFileRecord);
        armBatchResponses.get(externalObjectDirectoryId).setUploadFileFilenameProcessor(uploadFileFilenameProcessor);
    }

    private void createArmBatchResponseIfNotExists(Integer externalObjectDirectoryId) {
        if (!armBatchResponses.containsKey(externalObjectDirectoryId)) {
            ArmResponseBatchData armResponseBatchData = ArmResponseBatchData.builder()
                .externalObjectDirectoryId(externalObjectDirectoryId)
                .build();
            armBatchResponses.put(externalObjectDirectoryId, armResponseBatchData);
        }
    }
}
