package uk.gov.hmcts.darts.authorisation.service.impl;

public record GetAuthorisationResult(Integer userId, String userName,
                                     Integer roleId, String roleName,
                                     Integer permissionId, String permissionName) {

}
