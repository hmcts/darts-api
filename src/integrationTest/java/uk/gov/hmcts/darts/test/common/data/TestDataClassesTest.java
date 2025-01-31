package uk.gov.hmcts.darts.test.common.data;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

/**
 * A test to test the integrity of our data insertion classes. This class is useful to ensure we fail fast on
 * the test data classes being incompatible with the database constraints
 */
@Slf4j
class TestDataClassesTest extends IntegrationBase {

    @Autowired
    private DartsPersistence dartsPersistence;


    @ParameterizedTest(name = "testRequestMinimal - {0}")
    @MethodSource("persistableClassesProvider")
    void testRequestMinimal(Class<?> cls) throws Exception {
        Persistable<?, ?, ?> obj = (Persistable<?, ?, ?>) cls.getDeclaredConstructors()[0].newInstance();
        BuilderHolder<?, ?> retrieve = obj.someMinimalBuilderHolder();
        Object entity = retrieve.build().getEntity();

        dartsPersistence.getClass()
            .getMethod("save", entity.getClass())
            .invoke(dartsPersistence, entity);

    }


    static Stream<Arguments> persistableClassesProvider() throws ClassNotFoundException {
        return getPersistableClasses()
            .stream()
            .map(cls -> Arguments.of(cls));
    }

    private static List<Class<?>> getPersistableClasses() throws ClassNotFoundException {
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