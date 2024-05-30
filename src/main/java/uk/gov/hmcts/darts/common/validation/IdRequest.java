package uk.gov.hmcts.darts.common.validation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IdRequest<T> {

    private final T payload;

    private final Integer id;
}