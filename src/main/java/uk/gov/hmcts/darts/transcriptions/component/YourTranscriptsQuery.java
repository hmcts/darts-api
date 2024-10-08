package uk.gov.hmcts.darts.transcriptions.component;

import uk.gov.hmcts.darts.transcriptions.model.YourTranscriptsSummary;

import java.util.List;

public interface YourTranscriptsQuery {

    List<YourTranscriptsSummary> getRequesterTranscriptions(Integer userId, Boolean includeHiddenFromRequester);

    List<YourTranscriptsSummary> getApproverTranscriptions(Integer userId);

}
