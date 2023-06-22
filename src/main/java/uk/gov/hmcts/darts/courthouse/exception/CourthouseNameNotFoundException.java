package uk.gov.hmcts.darts.courthouse.exception;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CourthouseNameNotFoundException extends Exception {
    String courthouseName;

}
