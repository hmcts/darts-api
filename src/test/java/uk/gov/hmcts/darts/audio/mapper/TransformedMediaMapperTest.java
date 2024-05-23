package uk.gov.hmcts.darts.audio.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.model.GetTransformedMediaResponse;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;

import static org.assertj.core.api.Assertions.assertThat;

class TransformedMediaMapperTest {

    TransformedMediaMapper transformedMediaMapper = Mappers.getMapper(TransformedMediaMapper.class);

    @Test
    void testMapToGetTransformedMediaResponse() {

        // given
        var courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setId(22);

        var hearingEntity = new HearingEntity();
        hearingEntity.setId(11);
        hearingEntity.setCourtCase(courtCaseEntity);

        var mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setId(55);
        mediaRequestEntity.setHearing(hearingEntity);

        var transformedMediaEntity = new TransformedMediaEntity();
        transformedMediaEntity.setId(88);
        transformedMediaEntity.setOutputFilename("testOutputFileName");
        transformedMediaEntity.setOutputFormat(AudioRequestOutputFormat.MP3);
        transformedMediaEntity.setOutputFilesize(1000);
        transformedMediaEntity.setMediaRequest(mediaRequestEntity);

        // when
        GetTransformedMediaResponse getTransformedMediaResponse = transformedMediaMapper.mapToGetTransformedMediaResponse(transformedMediaEntity);

        // then
        assertThat(getTransformedMediaResponse.getId()).isEqualTo(88);
        assertThat(getTransformedMediaResponse.getMediaRequestId()).isEqualTo(55);
        assertThat(getTransformedMediaResponse.getCaseId()).isEqualTo(22);
        assertThat(getTransformedMediaResponse.getFileName()).isEqualTo("testOutputFileName");
        assertThat(getTransformedMediaResponse.getFileFormat()).isEqualTo("mp3");
        assertThat(getTransformedMediaResponse.getFileSizeBytes()).isEqualTo(1000);
    }
}