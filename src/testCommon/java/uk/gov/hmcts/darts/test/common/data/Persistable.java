package uk.gov.hmcts.darts.test.common.data;

public interface Persistable<M, T> {

    /**
     * NOTE: Objects created by this method will never populate the id field, as this is expected to be populated by the persistence layer upon save.
     *
     * @param testSpec a specification that defines the expected entity data
     * @return an entity that has state specified by the supplied test specification.
     */
    M fromSpec(T testSpec);

    /**
     * Return an entity that has only its non-null fields populated. All other fields should be expected to be null.
     *
     * <p>This represents the minimal persistable object that may be passed to an entityManager without throwing any constraint violation exceptions.
     *
     * <p>NOTE: Objects created by this method will never populate the id field, as this is expected to be populated by the persistence layer upon save.
     *
     * @return a minimally persistent instance of M
     */
    M someMinimal();

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
    M someMaximal();

}