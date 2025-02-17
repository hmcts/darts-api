package uk.gov.hmcts.darts.audio.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.log.service.AudioLoggerService;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddAudioMetaDataValidator implements Validator<AddAudioMetadataRequest> {

    private final RetrieveCoreObjectService retrieveCoreObjectService;
    private final AudioLoggerService audioLoggerService;

    @Override
    public void validate(AddAudioMetadataRequest addAudioMetadataRequest) {

        // attempt to resolve the courthouse
        try {
            retrieveCoreObjectService.retrieveCourthouse(addAudioMetadataRequest.getCourthouse());
        } catch (DartsApiException e) {
            if (CommonApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST.equals(e.getError())) {
                audioLoggerService.missingCourthouse(addAudioMetadataRequest.getCourthouse(), addAudioMetadataRequest.getCourtroom());
            }
            throw e;
        }
    }
}