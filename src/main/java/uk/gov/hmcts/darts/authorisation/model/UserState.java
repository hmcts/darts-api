package uk.gov.hmcts.darts.authorisation.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Set;

@Builder
@Value
public class UserState {

    @NonNull
    private Integer userId;
    @NonNull
    private String userName;
    @NonNull
    private Set<Role> roles;

}
