package uk.gov.hmcts.darts.audio.auditing;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

import static org.hibernate.event.spi.EventType.POST_UPDATE;

@Configuration
public class HibernateEventListenerRegistryConfiguration {

    private final EntityManagerFactory entityManagerFactory;

    @Autowired
    public HibernateEventListenerRegistryConfiguration(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @PostConstruct
    public void eventListenerRegistry() {
        EnversService enversService;
        EventListenerRegistry listenerRegistry;
        try (ServiceRegistryImplementor serviceRegistry = entityManagerFactory
            .unwrap(SessionFactoryImpl.class)
            .getServiceRegistry()) {

            enversService = serviceRegistry.getService(EnversService.class);
            listenerRegistry = serviceRegistry.getService(EventListenerRegistry.class);
        }

        assert listenerRegistry != null;

        listenerRegistry.setListeners(
            POST_UPDATE,
            new DartsEnversPostUpdateEventListener(enversService, new AuditExecutor()));
    }
}
