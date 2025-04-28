package uk.gov.hmcts.darts.datamanagement.service;

import java.util.List;

public interface InboundTranscriptionAnnotationDeleterProcessor {
    List<Long> markForDeletion(int hourThreshold, int batchSize);

    List<Long> markForDeletion(int batchSize);
}