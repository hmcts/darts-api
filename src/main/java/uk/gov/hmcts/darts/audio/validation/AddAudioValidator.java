package uk.gov.hmcts.darts.audio.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.component.validation.Validator;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AddAudioValidator implements Validator<AddAudioMetadataRequest> {

    private final RetrieveCoreObjectService retrieveCoreObjectService;

    private final AudioConfigurationProperties properties;

    @Value("${darts.audio.max-file-duration-minutes}")
    private Long audioDurationInMinutes;

    public void validate(AddAudioMetadataRequest addAudioMetadataRequest) {

        // attempt to resolve the court house
        retrieveCoreObjectService.retrieveCourthouse(addAudioMetadataRequest.getCourthouse());

        log.debug("Validated the court house {} exists", addAudioMetadataRequest.getCourthouse());

        // now check the white list for extension types
        Optional<String> found = properties.getAllowedMediaFormats().stream().filter(ext -> addAudioMetadataRequest.getFormat().equals(ext)
        ).findFirst();

        if (!found.isPresent()) {
            throw new DartsApiException(AudioApiError.UNEXPECTED_FILE_TYPE);
        }

        // check the duration
        OffsetDateTime startDate = addAudioMetadataRequest.getStartedAt();
        OffsetDateTime finishDate = addAudioMetadataRequest.getEndedAt();

        Duration difference = Duration.between(startDate, finishDate);
        long minutesDifference = difference.get(ChronoUnit.SECONDS) / 60;

        if (minutesDifference > audioDurationInMinutes) {
            throw new DartsApiException(AudioApiError.FILE_DURATION_OUT_OF_BOUNDS);
        }
    }
}