package uk.gov.hmcts.darts.authorisation.component;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;

import java.util.Set;

@Component
public interface Authorisation {

    void authoriseByCaseId(Integer caseId, Set<SecurityRoleEnum> securityRoles);

    void authoriseByHearingId(Integer hearingId, Set<SecurityRoleEnum> securityRoles);

    void authoriseByMediaRequestId(Integer mediaRequestId, Set<SecurityRoleEnum> securityRoles);

    void authoriseByMediaId(Integer mediaId, Set<SecurityRoleEnum> securityRoles);

    void authoriseByTranscriptionId(Integer transcriptionId, Set<SecurityRoleEnum> securityRoles);

}
