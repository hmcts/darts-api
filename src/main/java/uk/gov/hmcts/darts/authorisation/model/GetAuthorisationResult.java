package uk.gov.hmcts.darts.authorisation.model;

public record GetAuthorisationResult(Integer userId, String userName,
                                     Boolean globalAccess,
                                     Integer courthouseId,
                                     Integer roleId, String roleName,
                                     Integer permissionId, String permissionName) {

}
