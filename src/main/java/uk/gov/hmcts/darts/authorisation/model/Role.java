package uk.gov.hmcts.darts.authorisation.model;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Builder
@Value
@SuppressWarnings({"PMD.ShortClassName"})
public class Role {

    private Integer roleId;
    private String roleName;
    private Set<Permission> permissions;

}
