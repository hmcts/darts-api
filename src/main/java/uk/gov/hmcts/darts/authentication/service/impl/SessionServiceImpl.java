package uk.gov.hmcts.darts.authentication.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.authentication.component.SessionCache;
import uk.gov.hmcts.darts.authentication.model.Session;
import uk.gov.hmcts.darts.authentication.service.SessionService;

@Slf4j
@Component
@AllArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final SessionCache sessionCache;

    @Override
    public Session getSession(String sessionId) {
        return sessionCache.get(sessionId);
    }

    @Override
    public void putSession(String sessionId, Session session) {
        sessionCache.put(sessionId, session);
    }

}
