package uk.gov.hmcts.darts.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SecurityRoleEnum {

    COURT_MANAGER(1),
    COURT_CLERK(2),
    JUDGE(3),
    TRANSCRIPTION_COMPANY(4),
    LANGUAGE_SHOP(5);

    private final Integer id;

}
