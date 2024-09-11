package uk.gov.hmcts.darts.test.common.data.builder;

public interface BuilderRetrieve<T, M> {
    T build();

    M getBuilder();
}