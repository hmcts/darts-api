package uk.gov.hmcts.darts.hearings.mapper;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.hearings.model.HearingTranscriptModel;
import uk.gov.hmcts.darts.hearings.model.Transcript;

import java.util.ArrayList;
import java.util.List;

@Component
public class HearingTranscriptionMapper extends uk.gov.hmcts.darts.common.mapper.TranscriptionMapper<HearingTranscriptModel> {
    @Override
    protected HearingTranscriptModel createNewTranscription() {
        return new HearingTranscriptModel();
    }

    public <T> List<Transcript> getTranscriptList(List<T> modelLst) {
        List<Transcript> transcriptList = new ArrayList<>();
        for (T model : modelLst) {
            if (model instanceof Transcript) {
                transcriptList.add((Transcript) model);
            }
        }

        return transcriptList;
    }
}