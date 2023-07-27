package uk.gov.hmcts.darts.authorisation.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@SuppressWarnings({"PMD.ShortClassName"})
public class Role {

    private Integer roleId;
    private String roleName;
    List<Permission> permissions;

}
