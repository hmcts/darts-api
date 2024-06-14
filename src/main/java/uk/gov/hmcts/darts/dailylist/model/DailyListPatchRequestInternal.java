package uk.gov.hmcts.darts.dailylist.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyListPatchRequestInternal {

    Integer dailyListId;
    DailyListJsonObject dailyListJson;
}
