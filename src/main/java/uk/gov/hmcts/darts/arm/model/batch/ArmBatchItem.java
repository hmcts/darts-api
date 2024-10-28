package uk.gov.hmcts.darts.arm.model.batch;

import lombok.Data;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;

@Data
public class ArmBatchItem {
    private ExternalObjectDirectoryEntity sourceEod;
    private ExternalObjectDirectoryEntity armEod;
    private String previousManifestFile;
    private ObjectRecordStatusEntity previousStatus;
    private Boolean rawFilePushSuccessful;
    private ArchiveRecord archiveRecord;

    public void setArmEod(ExternalObjectDirectoryEntity armEod) {
        this.armEod = armEod;
        this.previousManifestFile = armEod.getManifestFile();
        this.previousStatus = armEod.getStatus();
    }

    public void undoManifestFileChange() {
        this.armEod.setManifestFile(this.previousManifestFile);
    }

    public boolean isRawFilePushNotNeededOrSuccessfulWhenNeeded() {
        return rawFilePushSuccessful == null || rawFilePushSuccessful;
    }
}
