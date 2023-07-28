package uk.gov.hmcts.darts.authorisation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.EqualsAndHashCode.Exclude;
import lombok.EqualsAndHashCode.Include;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@EqualsAndHashCode
@ToString
@SuppressWarnings({"PMD.ShortClassName"})
public class Role {

    @Include
    private Integer roleId;
    @Include
    private String roleName;
    @Exclude
    Set<Permission> permissions;

}
