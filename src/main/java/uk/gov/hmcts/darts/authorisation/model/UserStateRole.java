package uk.gov.hmcts.darts.authorisation.model;

import lombok.Builder;
import lombok.EqualsAndHashCode.Exclude;
import lombok.EqualsAndHashCode.Include;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

@Builder
@Value
@Jacksonized
public class UserStateRole {

    @Include
    @NonNull
    private Integer roleId;
    @Include
    @NonNull
    private String roleName;
    @NonNull
    private Boolean globalAccess;
    @Exclude
    private Set<Integer> courthouseIds;
    @Exclude
    @NonNull
    private Set<String> permissions;

}