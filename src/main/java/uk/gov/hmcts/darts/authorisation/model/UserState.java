package uk.gov.hmcts.darts.authorisation.model;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Builder
@Value
public class UserState {

    private Integer userId;
    private String userName;
    private Set<Role> roles;

}
