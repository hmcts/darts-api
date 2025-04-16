package uk.gov.hmcts.darts.common.component.validation;

import uk.gov.hmcts.darts.common.exception.DartsApiException;

/**
 * A generic validation interface.
 */
@FunctionalInterface
public interface Validator<T> {

    /**
     * Validate the provided {@code validatable}.
     *
     * <p>The general convention is to throw a {@code DartsApiException} if validation fails, but this is
     * not enforced by this interface.
     *
     * @throws DartsApiException if validation fails
     */
    void validate(T validatable);

}
