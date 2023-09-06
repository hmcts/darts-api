package uk.gov.hmcts.darts.authorisation.annotation;

import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Authorisation {

    ContextIdEnum contextId();

}
