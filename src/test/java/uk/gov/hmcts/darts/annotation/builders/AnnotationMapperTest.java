package uk.gov.hmcts.darts.annotation.builders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;

import java.time.OffsetDateTime;

import static java.time.OffsetDateTime.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnotationMapperTest {

    public static final OffsetDateTime SOME_OFFSET_DATE_TIME = parse("2020-01-01T00:00:00Z");
    public static final UserAccountEntity USER_ACCOUNT_ENTITY = new UserAccountEntity();

    @Mock
    private AuthorisationApi authorisationApi;
    @Mock
    private CurrentTimeHelper currentTimeHelper;

    private AnnotationMapper annotationMapper;

    @BeforeEach
    void setUp() {
        annotationMapper = new AnnotationMapper(authorisationApi, currentTimeHelper);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(SOME_OFFSET_DATE_TIME);
        when(authorisationApi.getCurrentUser()).thenReturn(USER_ACCOUNT_ENTITY);
        USER_ACCOUNT_ENTITY.setId(123);
    }

    @Test
    void mapsAnnotationToAnnotationEntityCorrectly() {
        var annotation = someAnnotation();

        assertThat(annotationMapper.mapFrom(annotation))
            .hasFieldOrPropertyWithValue("text", annotation.getComment())
            .hasFieldOrPropertyWithValue("deleted", false)
            .hasFieldOrPropertyWithValue("timestamp", SOME_OFFSET_DATE_TIME)
            .hasFieldOrPropertyWithValue("createdDateTime", SOME_OFFSET_DATE_TIME)
            .hasFieldOrPropertyWithValue("currentOwner", USER_ACCOUNT_ENTITY)
            .hasFieldOrPropertyWithValue("lastModifiedById", USER_ACCOUNT_ENTITY.getId())
            .hasFieldOrPropertyWithValue("createdById", USER_ACCOUNT_ENTITY.getId());
    }

    private Annotation someAnnotation() {
        return new Annotation();
    }
}
