package uk.gov.hmcts.darts.cases.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.cases.model.CaseTranscriptModel;
import uk.gov.hmcts.darts.cases.model.Transcript;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CaseTranscriptionMapper extends uk.gov.hmcts.darts.common.mapper.TranscriptionMapper<CaseTranscriptModel> {
    @Override
    protected CaseTranscriptModel createNewTranscription() {
        return new CaseTranscriptModel();
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