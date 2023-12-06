package uk.gov.hmcts.darts.usermanagement.component.validation;

public interface Validator<T> {

    void validate(T validatable);

}
