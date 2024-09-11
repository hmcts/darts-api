package uk.gov.hmcts.darts.common.data;

import org.apache.commons.lang3.ClassUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import uk.gov.hmcts.darts.test.common.data.MediaRequestTestData;
import uk.gov.hmcts.darts.test.common.data.Persistable;
import uk.gov.hmcts.darts.test.common.data.builder.BuilderRetrieve;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.test.common.data.persistence.Persistence;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestDataClassesTest extends IntegrationBase {

    private Map<Persistable<?>, Persistence<?>> testDataPersistanceMapping = new HashMap<>();

    @Autowired
    private DartsPersistence dartsPersistence;


    @Test
    public void testRequestMinimal() throws Exception {
        List<Class<?>> classList = getPersistableClasses();
        for(Class<?> cls : classList) {
            Persistable<?> obj = (Persistable<?>) cls.getConstructor().newInstance();
            BuilderRetrieve<?,?> retrieve = obj.someMinimal();
            Object entity = retrieve.build();

            dartsPersistence.getClass()
                .getMethod("save", entity.getClass()).invoke(dartsPersistence,
                                                                             entity);
        }
    }

    @Test
    public void testRequestMaximal() throws Exception {
        List<Class<?>> classList = getPersistableClasses();
        for(Class<?> cls : classList) {
            Persistable<?> obj = (Persistable<?>) cls.getConstructor().newInstance();
            BuilderRetrieve<?,?> retrieve = obj.someMaximal();
            Object entity = retrieve.build();
            dartsPersistence.getClass()
                .getMethod("save", entity.getClass()).invoke(dartsPersistence,
                                                             entity);
        }
    }

    private List<Class<?>> getPersistableClasses() throws Exception {
        List<Class<?>> classList = new ArrayList<>();
        BeanDefinitionRegistry bdr = new SimpleBeanDefinitionRegistry();
        ClassPathBeanDefinitionScanner s = new ClassPathBeanDefinitionScanner(bdr);

        TypeFilter tf_include_1 = new AssignableTypeFilter(Persistable.class);
        s.addIncludeFilter(tf_include_1);
        s.setIncludeAnnotationConfig(false);
        s.scan("uk.gov.hmcts.darts.test.common.data");

        String[] beans = bdr.getBeanDefinitionNames();
        for (String bean : beans) {
            classList.add(Class.forName(bdr.getBeanDefinition(bean).getBeanClassName()));
        }

        return classList;
    }
}