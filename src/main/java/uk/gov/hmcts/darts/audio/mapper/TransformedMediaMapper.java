package uk.gov.hmcts.darts.audio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mappings;
import org.mapstruct.ValueMapping;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audio.model.GetTransformedMediaResponse;
import uk.gov.hmcts.darts.audio.model.TransformedMediaDetailsDto;
import uk.gov.hmcts.darts.audiorequests.model.TransformedMediaDetails;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransformedMediaMapper {

    List<TransformedMediaDetails> mapToTransformedMediaDetails(List<TransformedMediaDetailsDto> transformedMediaDetailsDto);

    @Mappings({
        @Mapping(source = "outputFilename", target = "fileName"),
        @Mapping(source = "outputFormat.extension", target = "fileFormat"),
        @Mapping(source = "outputFilesize", target = "fileSizeBytes"),
        @Mapping(source = "mediaRequest.id", target = "mediaRequestId"),
        @Mapping(source = "mediaRequest.hearing.courtCase.id", target = "caseId"),
    })
    GetTransformedMediaResponse mapToGetTransformedMediaResponse(TransformedMediaEntity transformedMediaEntity);

    @ValueMapping(source = "DELETED", target = MappingConstants.NULL)
    uk.gov.hmcts.darts.audiorequests.model.MediaRequestStatus map(MediaRequestStatus status);
}
