package uk.gov.hmcts.darts.authorisation.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

@Builder
@Jacksonized
@Value
public class UserState {

    @NonNull
    private Integer userId;
    @NonNull
    private String userName;
    @NonNull
    private Set<UserStateRole> roles;
    @NonNull
    private Boolean isActive;

}