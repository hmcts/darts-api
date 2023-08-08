package uk.gov.hmcts.darts.authorisation.model;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class Permission {

    private Integer permissionId;
    private String permissionName;

}
