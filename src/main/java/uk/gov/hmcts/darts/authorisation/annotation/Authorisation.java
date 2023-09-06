package uk.gov.hmcts.darts.authorisation.annotation;

import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.CASE_ID;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Authorisation {

    public ContextIdEnum contextId() default CASE_ID;

}
