package uk.gov.hmcts.darts.arm.service;

import java.util.List;

public interface InboundTranscriptionAndAnnotationDeleterProcessor {
    List<Integer> processDeletionIfAfterHours(int batch, int hourThreshold);

    List<Integer> processDeletionIfAfterHours(int batch);
}