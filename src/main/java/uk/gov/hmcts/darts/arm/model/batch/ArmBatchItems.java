package uk.gov.hmcts.darts.arm.model.batch;

import uk.gov.hmcts.darts.arm.model.ArchiveRecord;

import java.util.ArrayList;
import java.util.List;


public class ArmBatchItems {
    private final List<ArmBatchItem> items = new ArrayList<>();

    public void add(ArmBatchItem batchItem) {
        items.add(batchItem);
    }

    public List<ArmBatchItem> getSuccessful() {
        return items.stream().filter(
            batchItem -> batchItem.isRawFilePushNotNeededOrSuccessfulWhenNeeded() && batchItem.getArchiveRecord() != null).toList();
    }

    public List<ArchiveRecord> getArchiveRecords() {
        return getSuccessful().stream().map(ArmBatchItem::getArchiveRecord).toList();
    }
}
