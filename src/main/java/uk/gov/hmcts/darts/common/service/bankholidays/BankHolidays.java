package uk.gov.hmcts.darts.common.service.bankholidays;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BankHolidays {
    static final String ENGLAND_AND_WALES = "england-and-wales";

    @JsonProperty(ENGLAND_AND_WALES)
    public Division englandAndWales;

}
