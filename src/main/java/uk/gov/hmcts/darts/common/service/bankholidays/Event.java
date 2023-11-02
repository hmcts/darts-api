package uk.gov.hmcts.darts.common.service.bankholidays;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {

    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("title")
    private String title;
}
