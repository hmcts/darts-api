package uk.gov.hmcts.darts.task.runner.dailylist.utilities.deserializer;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class OffsetDateTimeTypeDeserializer extends JsonDeserializer<OffsetDateTime> {
    public static OffsetDateTime getLOffsetDate(String offsetDate) {
        return OffsetDateTime.parse(offsetDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    @Override
    public OffsetDateTime deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException, JacksonException {
        return getLOffsetDate(parser.getText());
    }
}

