package uk.gov.hmcts.darts.authorisation.model;

import lombok.Builder;
import lombok.EqualsAndHashCode.Exclude;
import lombok.EqualsAndHashCode.Include;
import lombok.NonNull;
import lombok.Value;

import java.util.Set;

@Builder
@Value
@SuppressWarnings({"PMD.ShortClassName"})
public class Role {

    @Include
    @NonNull
    private Integer roleId;
    @Include
    @NonNull
    private String roleName;
    @NonNull
    private Boolean globalAccess;
    @Exclude
    private Set<Integer> courthouses;
    @Exclude
    @NonNull
    private Set<String> permissions;

}
