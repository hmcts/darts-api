package uk.gov.hmcts.darts.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SecurityGroupEnum {
    SUPER_ADMIN("SUPER_ADMIN"),
    SUPER_USER("SUPER_USER");

    private final String name;

}