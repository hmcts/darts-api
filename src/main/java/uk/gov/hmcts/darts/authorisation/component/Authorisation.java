package uk.gov.hmcts.darts.authorisation.component;

import org.springframework.stereotype.Component;

@Component
public interface Authorisation {

    void authoriseByCaseId(Integer caseId);

    void authoriseByHearingId(Integer hearingId);

    void authoriseByMediaRequestId(Integer mediaRequestId);

    void authoriseByMediaId(Integer mediaId);

    void authoriseByTranscriptionId(Integer transcriptionId);

}
