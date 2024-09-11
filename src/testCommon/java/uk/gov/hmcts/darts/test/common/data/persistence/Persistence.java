package uk.gov.hmcts.darts.test.common.data.persistence;

public interface Persistence<T> {
    T save (T t);
}