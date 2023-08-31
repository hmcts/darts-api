package uk.gov.hmcts.darts.courthouses.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

@AllArgsConstructor
@Getter
public class CourthouseCodeNotMatchException extends Exception {
    CourthouseEntity databaseCourthouse;
    Integer receivedCourthouseCode;
    String receivedCourthouseName;

}
