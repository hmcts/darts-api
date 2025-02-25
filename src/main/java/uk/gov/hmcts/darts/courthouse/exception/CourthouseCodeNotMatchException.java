package uk.gov.hmcts.darts.courthouse.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

@AllArgsConstructor
@Getter
public class CourthouseCodeNotMatchException extends Exception {
    private CourthouseEntity databaseCourthouse;
    private Integer receivedCourthouseCode;
    private String receivedCourthouseName;

}
