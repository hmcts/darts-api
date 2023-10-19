package uk.gov.hmcts.darts.authorisation.model;

public record GetAuthorisationResult(Integer userId, String userName,
                                     Integer roleId, String roleName,
                                     Integer permissionId, String permissionName) {

}
