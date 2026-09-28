package uk.gov.hmcts.darts.audio.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.api.AudioApi;
import uk.gov.hmcts.darts.audio.service.AudioIngestionService;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;

@Service
@RequiredArgsConstructor
public class AudioApiImpl implements AudioApi {

    private final AudioTransformationService audioTransformationService;
    private final AudioIngestionService audioIngestionService;

    @Override
    public void handleKedaInvocationForMediaRequests() {
        audioTransformationService.handleKedaInvocationForMediaRequests();
    }

    @Override
    public void handleKedaInvocationForAudioIngestion() {
        audioIngestionService.process();
    }

}
