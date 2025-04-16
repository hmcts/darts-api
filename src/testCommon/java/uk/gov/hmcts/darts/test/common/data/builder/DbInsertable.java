package uk.gov.hmcts.darts.test.common.data.builder;

/**
 * An object that can be called to get hold of an insertable entity.
*/
@FunctionalInterface
public interface DbInsertable<M> {

    /**
     * The entity that can be inserted directly into the database.
     * @return The entity that can be inserted directly into the database
     */
    M getEntity();
}