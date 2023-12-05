package uk.gov.hmcts.darts.usermanagement.component.validation.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.usermanagement.component.validation.Validator;
import uk.gov.hmcts.darts.usermanagement.exception.UserManagementError;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroup;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class SecurityGroupCreationValidation implements Validator<SecurityGroup> {

    private final SecurityGroupRepository securityGroupRepository;

    @Override
    public void validate(SecurityGroup securityGroup) {
        String name = securityGroup.getName();

        securityGroupRepository.findByGroupName(name)
            .ifPresent(existingGroup -> {
                throw new DartsApiException(
                    UserManagementError.DUPLICATE_SECURITY_GROUP_NAME_NOT_PERMITTED,
                    "Attempt to create group that already exists",
                    Collections.singletonMap("existing_group_id", existingGroup.getId())
                );
            });
    }

}
