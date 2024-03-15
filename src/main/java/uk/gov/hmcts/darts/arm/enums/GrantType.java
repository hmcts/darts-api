package uk.gov.hmcts.darts.arm.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum GrantType {

    PASSWORD("password");

    private final String value;
}
