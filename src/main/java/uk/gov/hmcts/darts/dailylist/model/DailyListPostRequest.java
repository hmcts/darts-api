package uk.gov.hmcts.darts.dailylist.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyListPostRequest {

    String sourceSystem;
    String courthouse;
    LocalDate hearingDate;
    String dailyListXml;
    String uniqueId;
    OffsetDateTime publishedDateTime;
    DailyListJsonObject dailyListJson;
}
