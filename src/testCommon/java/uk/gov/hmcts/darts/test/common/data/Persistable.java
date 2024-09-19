package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.test.common.data.builder.BuilderHolder;

public interface Persistable<M extends BuilderHolder<?,?>, T, B> {

    /**
     * Return an entity that has only its non-null fields populated. All other fields should be expected to be null.
     *
     * <p>This represents the minimal persistable object that may be passed to an entityManager without throwing any constraint violation exceptions.
     *
     * <p>NOTE: Objects created by this method will never populate the id field, as this is expected to be populated by the persistence layer upon save.
     *
     * @return a minimally persistent instance of M
     */
    T someMinimal();

    /**
     * Return an entity that has all of its fields populated.
     *
     * <p>This should always be a superset of someMinimal(), and in cases where the entity has non-null constraints on every field the implementation should be
     * identical to someMinimal().
     *
     * <p>NOTE: Objects created by this method will never populate the id field, as this is expected to be populated by the persistence layer upon save.
     *
     * @return a maximally persistent instance of M
     */
    default T someMaximal() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Returns a builder holder with the minimal builder.
     * @return The minimal builder holder
     */
    M someMinimalBuilderHolder();

    /**
     * Returns a builder holder with the maximum builder.
     * @return The minimal builder holder
     */
    //TODO: Remove this once implemented in each sub class
    default M someMaximumBuilderHolder() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Returns a the minimum builder.
     * @return The minimal builder holder
     */
    B someMinimalBuilder();

    /**
     * Returns a the maximum builder.
     * @return The minimal builder holder
     */
    //TODO: Remove this once implemented in each sub class
    default B someMaximumBuilder() {
        throw new UnsupportedOperationException("Not implemented");
    }
}