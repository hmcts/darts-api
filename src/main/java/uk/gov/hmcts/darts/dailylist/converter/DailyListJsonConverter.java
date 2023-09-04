package uk.gov.hmcts.darts.dailylist.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.dailylist.exception.DailyListError;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyListJsonConverter implements Converter<String, DailyListJsonObject> {

    private final ObjectMapper objectMapper;

    @Override
    public DailyListJsonObject convert(String from) {
        if (StringUtils.isBlank(from)) {
            return null;
        }
        try {
            return objectMapper.readValue(from, DailyListJsonObject.class);
        } catch (JsonProcessingException e) {
            log.error("An Error has occurred trying to parse the json ", e);
            throw new DartsApiException(DailyListError.FAILED_TO_PROCESS_DAILYLIST);
        }
    }
}
