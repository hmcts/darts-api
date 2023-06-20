package uk.gov.hmcts.darts.courthouse.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.darts.common.entity.Courthouse;

@AllArgsConstructor
@Getter
public class CourthouseCodeNotMatchException extends Exception {
    Courthouse courthouse;
    Short newCourthouseCode;

}
