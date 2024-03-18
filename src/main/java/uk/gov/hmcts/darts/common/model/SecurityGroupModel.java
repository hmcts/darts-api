package uk.gov.hmcts.darts.common.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public final class SecurityGroupModel {
    private String name;
    private String displayName;
    private String description;
    private boolean useInterpreter;

    private Integer roleId;
}
