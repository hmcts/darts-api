package uk.gov.hmcts.darts.authentication.component;

import uk.gov.hmcts.darts.authentication.model.Session;

public interface SessionCache {

    void put(String sessionId, Session session);

    Session get(String sessionId);

    Session remove(String sessionId);
}
