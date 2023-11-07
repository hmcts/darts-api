package uk.gov.hmcts.darts.audio.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.api.AudioApi;
import uk.gov.hmcts.darts.audio.service.AudioTransformationService;

@Service
@RequiredArgsConstructor
public class AudioApiImpl implements AudioApi {

    private final AudioTransformationService audioTransformationService;

    @Override
    public void handleKedaInvocationForMediaRequests() {
        audioTransformationService.handleKedaInvocationForMediaRequests();
    }

}
