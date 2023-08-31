package uk.gov.hmcts.darts.courthouses.exception;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CourthouseNameNotFoundException extends Exception {
    String courthouseName;

}
