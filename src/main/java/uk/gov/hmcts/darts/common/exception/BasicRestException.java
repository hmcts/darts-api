package uk.gov.hmcts.darts.common.exception;

import lombok.Getter;
import lombok.Setter;

/*
 * @deprecated in favour of RFC-7807 compliant handler uk.gov.hmcts.darts.common.exception.ExceptionHandler
 * TODO: Eliminate this class (DMP-516)
 */
@Deprecated
@Setter
@Getter
public class BasicRestException {
    String code;
    String message;
}
