package uk.gov.hmcts.darts.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TasksOpenApiContractTest {

    private static final String EDIT_CRON_EXPRESSION_PATH = "/admin/automated-tasks/1/edit-cron-expression";
    private static final String APPLICATION_JSON = "application/json";
    private static final OpenApiInteractionValidator VALIDATOR =
        OpenApiInteractionValidator.createForSpecificationUrl(
            TasksOpenApiContractTest.class
                .getResource("/openapi/tasks.yaml")
                .toExternalForm()
        ).build();

    @ParameterizedTest(name = "{0} should accept a valid cron expression")
    @MethodSource("editCronExpressionRequestMethods")
    void openApi_ShouldReturnNoError_WhenValidCronExpressionUsed(Request.Method method) {
        Request request = createEditCronExpressionRequest(method, "0 0 10 * * *");

        ValidationReport report = VALIDATOR.validateRequest(request);

        assertTrue(report.getMessages().isEmpty(), () -> "Expected no validation errors but got: " + report.getMessages());
    }

    @ParameterizedTest(name = "{0} should reject {1}")
    @MethodSource("invalidCronExpressionRequests")
    void openApi_ShouldReturnError_WhenInvalidCronExpressionUsed(Request.Method method, String testName, String cronExpression) {
        Request request = createEditCronExpressionRequest(method, cronExpression);

        ValidationReport report = VALIDATOR.validateRequest(request);

        assertFalse(report.getMessages().isEmpty(), () -> "Expected validation errors for " + testName);
    }

    private static Stream<Request.Method> editCronExpressionRequestMethods() {
        return Stream.of(
            Request.Method.PATCH,
            Request.Method.POST
        );
    }

    private static Stream<Arguments> invalidCronExpressionRequests() {
        return editCronExpressionRequestMethods()
            .flatMap(method -> Stream.of(
                Arguments.of(method, "empty cron expression", ""),
                Arguments.of(method, "cron expression with invalid characters", "0 0 10 * * * !"),
                Arguments.of(method, "cron expression over the maximum length", "0".repeat(257))
            ));
    }

    private static Request createEditCronExpressionRequest(Request.Method method, String cronExpression) {
        return new SimpleRequest.Builder(method, EDIT_CRON_EXPRESSION_PATH)
            .withContentType(APPLICATION_JSON)
            .withBody("""
                          { "cron_expression": "%s" }
                          """.formatted(cronExpression))
            .build();
    }
}
