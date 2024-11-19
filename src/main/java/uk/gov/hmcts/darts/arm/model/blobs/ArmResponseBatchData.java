package uk.gov.hmcts.darts.arm.model.blobs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseCreateRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseInvalidLineRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecord;
import uk.gov.hmcts.darts.arm.util.files.CreateRecordFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.InvalidLineFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
public class ArmResponseBatchData {
    private int externalObjectDirectoryId;

    private ArmResponseCreateRecord armResponseCreateRecord;
    @Builder.Default
    private List<ArmResponseInvalidLineRecord> armResponseInvalidLineRecords = new ArrayList<>();
    private ArmResponseUploadFileRecord armResponseUploadFileRecord;

    private CreateRecordFilenameProcessor createRecordFilenameProcessor;
    @Builder.Default
    private List<InvalidLineFileFilenameProcessor> invalidLineFileFilenameProcessors = new ArrayList<>();
    private UploadFileFilenameProcessor uploadFileFilenameProcessor;
}
