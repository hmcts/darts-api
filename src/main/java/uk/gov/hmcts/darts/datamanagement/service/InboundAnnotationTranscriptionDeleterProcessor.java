package uk.gov.hmcts.darts.datamanagement.service;

import java.util.List;

public interface InboundAnnotationTranscriptionDeleterProcessor {
    List<Integer> markForDeletion(int hourThreshold);

    List<Integer> markForDeletion();
}