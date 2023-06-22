package uk.gov.hmcts.darts.dailylist.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyListPostRequest {

    String sourceSystem;
    DailyList dailyList;
}
