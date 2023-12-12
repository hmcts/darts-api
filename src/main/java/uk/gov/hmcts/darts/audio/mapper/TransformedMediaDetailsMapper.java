package uk.gov.hmcts.darts.audio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.model.TransformedMediaDetailsDto;
import uk.gov.hmcts.darts.audiorequests.model.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.TransformedMediaDetails;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransformedMediaDetailsMapper {

    List<TransformedMediaDetails> mapToTransformedMediaDetails(List<TransformedMediaDetailsDto> transformedMediaDetailsDto);

    @ValueMapping(source = "DELETED", target = MappingConstants.NULL)
    MediaRequestStatus map(AudioRequestStatus status);
}
