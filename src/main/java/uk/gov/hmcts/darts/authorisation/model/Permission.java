package uk.gov.hmcts.darts.authorisation.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder
@Value
public class Permission {

    @NonNull
    private String permissionName;

}
