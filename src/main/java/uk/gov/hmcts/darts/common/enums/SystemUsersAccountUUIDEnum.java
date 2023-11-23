package uk.gov.hmcts.darts.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
@AllArgsConstructor
@Getter
public enum SystemUsersAccountUUIDEnum {

    HOUSE_KEEPING("ecfd1f14-c9b6-4f15-94c7-cc60e53f2c7a");
    private final String uuid;
}
