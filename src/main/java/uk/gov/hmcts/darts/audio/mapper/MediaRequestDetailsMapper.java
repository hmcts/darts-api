package uk.gov.hmcts.darts.audio.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Mappings;
import org.mapstruct.ValueMapping;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audio.model.EnhancedMediaRequestInfo;
import uk.gov.hmcts.darts.audiorequests.model.MediaRequestDetails;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MediaRequestDetailsMapper {

    List<MediaRequestDetails> map(List<EnhancedMediaRequestInfo> enhancedMediaRequestInfoList);

    @Mappings({
        @Mapping(source = "mediaRequestStartTs", target = "startTs"),
        @Mapping(source = "mediaRequestEndTs", target = "endTs")
    })
    MediaRequestDetails map(EnhancedMediaRequestInfo enhancedMediaRequestInfo);

    @ValueMapping(source = "DELETED", target = MappingConstants.NULL)
    uk.gov.hmcts.darts.audiorequests.model.MediaRequestStatus map(MediaRequestStatus status);
}
