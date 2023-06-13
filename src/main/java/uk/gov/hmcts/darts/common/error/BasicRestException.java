package uk.gov.hmcts.darts.common.error;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BasicRestException {
    String code;
    String message;
}
