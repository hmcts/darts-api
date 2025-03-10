package uk.gov.hmcts.darts.datamanagement.service;

import java.util.List;

public interface InboundTranscriptionAnnotationDeleterProcessor {
    List<Integer> markForDeletion(int hourThreshold, int batchSize);

    List<Integer> markForDeletion(int batchSize);
}