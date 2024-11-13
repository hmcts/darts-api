package uk.gov.hmcts.darts.arm.service;

import java.util.List;

public interface UnstructuredTranscriptionAndAnnotationDeleterProcessor {
    List<Integer> markForDeletion(int weeksBeforeCurrentDateInUnstructured, int hoursBeforeCurrentDateInArm, int batchSize);

    List<Integer> markForDeletion(int batchSize);
}