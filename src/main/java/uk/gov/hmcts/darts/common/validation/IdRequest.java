package uk.gov.hmcts.darts.common.validation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class IdRequest<T, N extends Number> {

    private final T payload;

    private final N id;
}