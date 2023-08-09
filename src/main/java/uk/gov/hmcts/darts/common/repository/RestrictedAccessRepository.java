package uk.gov.hmcts.darts.common.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean
public interface RestrictedAccessRepository<T, ID> extends Repository<T, ID> {

    String JPA_SECURITY_FRAGMENT = """
            JOIN securityGroup.courthouseEntities courthouse
            JOIN userAccount.securityGroupEntities
        WHERE userAccount.emailAddress = :#{ @userIdentityImpl.getIdentity() }
        """;

    // Or the alternative (but less flexible):
    //    String JPA_SECURITY_FRAGMENT = """
    //            JOIN securityGroup.courthouseEntities courthouse
    //            JOIN userAccount.securityGroupEntities
    //        WHERE userAccount.emailAddress = ?#{ principal.claims.get("emails").get(0) }
    //        """;

}
