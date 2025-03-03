package uk.gov.hmcts.darts.arm.model.batch;

import lombok.Getter;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ArmBatchItems {
    @Getter
    private final List<ArmBatchItem> items = Collections.synchronizedList(new ArrayList<>());

    public void add(ArmBatchItem batchItem) {
        items.add(batchItem);
    }

    public List<ArmBatchItem> getSuccessful() {
        return items.stream().filter(
            batchItem -> batchItem.isRawFilePushNotNeededOrSuccessfulWhenNeeded() && batchItem.getArchiveRecord() != null).toList();
    }

    public List<ArmBatchItem> getFailed() {
        return items.stream().filter(
            batchItem -> !batchItem.isRawFilePushNotNeededOrSuccessfulWhenNeeded() || batchItem.getArchiveRecord() == null).toList();
    }

    public List<ArchiveRecord> getArchiveRecords() {
        return getSuccessful().stream().map(ArmBatchItem::getArchiveRecord).toList();
    }
}
