package uk.gov.hmcts.darts.test.common.data.builder;

/**
 * Represents an object that holds a builder object that builds objects and gets hold of the built object to be inserted into the database.
 */
public interface BuilderHolder<T extends DbInsertable<?>, M> {
    /**
     * Return the built object that is due to be inserted into the database.
     * @return the built object
     */
    T build();

    /**
     * The builder object that is used to build the object.
     * @return The builder object
     */
    M getBuilder();
}