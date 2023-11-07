package uk.gov.hmcts.darts.authorisation.annotation;

import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Authorisation {

    boolean bodyAuthorisation() default false;

    ContextIdEnum contextId();

    SecurityRoleEnum[] securityRoles();

    SecurityRoleEnum[] globalAccessSecurityRoles();

}
