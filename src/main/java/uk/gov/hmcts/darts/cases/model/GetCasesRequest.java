package uk.gov.hmcts.darts.cases.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GetCasesRequest {

    String courthouse;
    String courtroom;
    LocalDate date;

}
