package uk.gov.hmcts.darts.arm.service;

import java.util.List;

public interface UnstructuredTranscriptionAndAnnotationDeleterProcessor {
    List<Integer> processDeletionIfPreceding(int batch, int weeksBeforeCurrentDate);

    List<Integer> processDeletionIfPreceding(int batch);
}