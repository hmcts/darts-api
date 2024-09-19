package uk.gov.hmcts.darts.test.common.data;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import uk.gov.hmcts.darts.test.common.data.builder.BuilderHolder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import java.util.ArrayList;
import java.util.List;

/**
 * A test to test the integrity of our data insertion classes. This class is useful to ensure we fail fast on
 * the test data classes being incompatible with the database constraints
 */
class TestDataClassesTest extends IntegrationBase {

    @Autowired
    private DartsPersistence dartsPersistence;


    @Test
    void testRequestMinimal() throws Exception {
        List<Class<?>> classList = getPersistableClasses();
        for (Class<?> cls : classList) {
            Persistable<?,?,?> obj = (Persistable<?,?,?>) cls.getDeclaredConstructors()[0].newInstance();
            BuilderHolder<?,?> retrieve = obj.someMinimalBuilderHolder();
            Object entity = retrieve.build().getEntity();

            dartsPersistence.getClass()
                .getMethod("save", entity.getClass()).invoke(dartsPersistence,
                                                                             entity);
        }
    }

    @Test
    @SuppressWarnings("PMD.EmptyCatchBlock")
    void testRequestMaximal() throws Exception {
        List<Class<?>> classList = getPersistableClasses();
        for (Class<?> cls : classList) {
            Persistable<?, ?, ?> obj = (Persistable<?, ?, ?>) cls.getDeclaredConstructors()[0].newInstance();
            try {
                BuilderHolder<?,?> retrieve = obj.someMaximumBuilderHolder();
                Object entity = retrieve.build().getEntity();

                dartsPersistence.getClass()
                    .getMethod("save", entity.getClass()).invoke(dartsPersistence,
                                                                 entity);
            } catch (UnsupportedOperationException unsupportedOperationException) {
                // lets pass if the maximal method is not yet implemented
            }
        }
    }

    private List<Class<?>> getPersistableClasses() throws ClassNotFoundException {
        List<Class<?>> classList = new ArrayList<>();
        BeanDefinitionRegistry bdr = new SimpleBeanDefinitionRegistry();
        ClassPathBeanDefinitionScanner classPathBeanDefinitionScanner = new ClassPathBeanDefinitionScanner(bdr);

        TypeFilter filter = new AssignableTypeFilter(Persistable.class);
        classPathBeanDefinitionScanner.addIncludeFilter(filter);
        classPathBeanDefinitionScanner.setIncludeAnnotationConfig(false);
        classPathBeanDefinitionScanner.scan("uk.gov.hmcts.darts.test.common.data");

        String[] beans = bdr.getBeanDefinitionNames();
        for (String bean : beans) {
            classList.add(Class.forName(bdr.getBeanDefinition(bean).getBeanClassName()));
        }

        return classList;
    }
}