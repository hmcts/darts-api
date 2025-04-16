package uk.gov.hmcts.darts.dailylist.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.dailylist.exception.DailyListError;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequestInternal;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListRequest;

@Mapper(componentModel = "spring")
@FunctionalInterface
public interface DailyListPostRequestMapper {

    ObjectMapperConfig OBJECT_MAPPER_CONFIG = new ObjectMapperConfig();
    ObjectMapper OBJECT_MAPPER = OBJECT_MAPPER_CONFIG.objectMapper();

    @Mappings({
        @Mapping(source = "xmlDocument", target = "dailyListXml"),
        @Mapping(source = "jsonString", target = "dailyListJson", qualifiedByName = "DailyListJsonMap"),
        @Mapping(source = "publishedTs", target = "publishedDateTime"),
    })
    DailyListPostRequestInternal map(PostDailyListRequest postDailyListRequest);

    @Named("DailyListJsonMap")
    default DailyListJsonObject toJsonObject(String jsonString) {
        if (StringUtils.isBlank(jsonString)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(jsonString, DailyListJsonObject.class);
        } catch (JsonProcessingException ex) {
            throw new DartsApiException(DailyListError.FAILED_TO_PROCESS_DAILYLIST, ex);
        }
    }

}
