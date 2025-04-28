package uk.gov.hmcts.darts.arm.service;

import java.util.List;

public interface UnstructuredTranscriptionAndAnnotationDeleterProcessor {
    List<Long> markForDeletion(int weeksBeforeCurrentDateInUnstructured, int hoursBeforeCurrentDateInArm, int batchSize);

    List<Long> markForDeletion(int batchSize);
}