package uk.gov.hmcts.darts.arm.service;

import java.util.List;

public interface InboundAnnotationTranscriptionDeleterProcessor {
    List<Integer> processDeletionIfPreceding(int batch, int hourThreshold);

    List<Integer> processDeletionIfPreceding(int batch);
}