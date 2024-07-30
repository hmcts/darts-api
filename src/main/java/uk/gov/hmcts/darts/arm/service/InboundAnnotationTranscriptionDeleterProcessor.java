package uk.gov.hmcts.darts.arm.service;

import java.util.List;

public interface InboundAnnotationTranscriptionDeleterProcessor {
    List<Integer> markForDeletion(int hourThreshold);

    List<Integer> markForDeletion();
}