package uk.gov.hmcts.darts.common.service.bankholidays;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {

    @JsonProperty("date")
    public LocalDate date;

    @JsonProperty("title")
    public String title;
}
