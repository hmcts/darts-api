package uk.gov.hmcts.darts.audio.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.log.service.AudioLoggerService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddAudioMetaDataValidatorTest {

    @Mock
    private RetrieveCoreObjectService retrieveCoreObjectService;
    @Mock
    private AudioConfigurationProperties properties;
    @Mock
    private AudioLoggerService audioLoggerService;

    @InjectMocks
    private AddAudioMetaDataValidator addAudioMetaDataValidator;


    @Test
    void validate_missingCourtHouse_shouldErrorAndLog() {
        AddAudioMetadataRequest request = new AddAudioMetadataRequest();
        request.setCourthouse("INVALID");
        request.setCourtroom("COURTROOM_123");
        DartsApiException expectedException = new DartsApiException(CommonApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST);
        when(retrieveCoreObjectService.retrieveCourthouse("INVALID")).thenThrow(expectedException);

        DartsApiException actualException = assertThrows(DartsApiException.class, () -> addAudioMetaDataValidator.validate(request));

        assertThat(actualException).isEqualTo(expectedException);
        verify(audioLoggerService).missingCourthouse("INVALID", "COURTROOM_123");
    }


    @Test
    void validate_hasCourtHouse_shouldNotErrorOrLog() {
        ReflectionTestUtils.setField(addAudioMetaDataValidator, "fileSizeThreshold", DataSize.of(10, DataUnit.GIGABYTES));
        ReflectionTestUtils.setField(addAudioMetaDataValidator, "maxAllowableAudioDuration", Duration.ofDays(1));

        AddAudioMetadataRequest request = new AddAudioMetadataRequest();
        request.setCourthouse("INVALID");
        request.setCourtroom("COURTROOM_123");
        request.setFormat("mp3");
        request.setFileSize(123L);
        request.setStartedAt(OffsetDateTime.now());
        request.setEndedAt(OffsetDateTime.now().plusHours(1));
        when(properties.getAllowedMediaFormats()).thenReturn(List.of("mp3"));

        addAudioMetaDataValidator.validate(request);

        verifyNoInteractions(audioLoggerService);
    }
}
