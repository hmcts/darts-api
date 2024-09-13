package uk.gov.hmcts.darts.test.common.data.builder;

/**
 * An object that can be called to get hold of an insertable entity
*/
public interface DbInsertable <M> {
    M getDbInsertable();
}