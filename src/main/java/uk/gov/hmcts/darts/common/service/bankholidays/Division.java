package uk.gov.hmcts.darts.common.service.bankholidays;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Division {

    @JsonProperty("events")
    public List<Event> events;

}
