package uk.gov.hmcts.darts.authentication.service;

import uk.gov.hmcts.darts.authentication.model.Session;

public interface SessionService {

    Session getSession(String sessionId);

    void putSession(String sessionId, Session session);

    Session dropSession(String sessionId);
}
