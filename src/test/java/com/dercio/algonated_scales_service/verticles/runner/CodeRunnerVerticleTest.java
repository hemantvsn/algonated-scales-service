package com.dercio.algonated_scales_service.verticles.runner;

import com.dercio.algonated_scales_service.response.Response;
import com.dercio.algonated_scales_service.runner.CodeOptions;
import com.dercio.algonated_scales_service.runner.CodeRunnerSummary;
import com.dercio.algonated_scales_service.verticles.CodeRegisterVerticle;
import com.dercio.algonated_scales_service.verticles.ScalesAnalyticsVerticle;
import com.dercio.algonated_scales_service.verticles.VerticleAddresses;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class CodeRunnerVerticleTest {

    private static final Vertx vertx = Vertx.vertx();

    @BeforeAll
    public static void prepare(VertxTestContext testContext) {
        vertx.deployVerticle(new CodeRegisterVerticle());
        vertx.deployVerticle(new ScalesAnalyticsVerticle());
        vertx.deployVerticle(
                new CodeRunnerVerticle(),
                testContext.succeeding(id -> testContext.completeNow())
        );
    }

    @Test
    @DisplayName("Compiles and runs a simple request")
    void compileAndRunSimpleRequest(VertxTestContext testContext) {
        List<Double> weights = List.of(
                1.0,
                2.0,
                3.0,
                4.0,
                5.0
        );
        vertx.eventBus().<Response>request(
                VerticleAddresses.CODE_RUNNER_CONSUMER.toString(),
                simpleCodeOptions(),
                messageReply -> testContext.verify(() -> {
                    Response response = messageReply.result().body();
                    assertTrue(response.isSuccess());
                    assertEquals("Compile and Run was a success", response.getConsoleOutput());
                    assertEquals(List.of(1, 1, 0, 1, 1), response.getResult());
                    assertEquals(weights, response.getData());
                    assertEquals(simpleCodeSummary(), response.getSummary());
                    assertEquals(Collections.emptyList(), response.getSolutions());
                    testContext.completeNow();
                })
        );
    }

    @Test
    @DisplayName("Request fails when there is an illegal import")
    void illegalImportFound(VertxTestContext testContext) {
        var options = simpleCodeOptions();
        options.setImportsAllowed(Collections.singletonList("import java.util.ArrayList;"));
        vertx.eventBus().<Response>request(
                VerticleAddresses.CODE_RUNNER_CONSUMER.toString(),
                options,
                messageReply -> testContext.verify(() -> {
                    assertTrue(messageReply.failed());
                    assertEquals("Please remove the following imports:\n[import java.util.List;]",
                            messageReply.cause().getMessage());
                    testContext.completeNow();
                })
        );
    }

    @Test
    @DisplayName("Request fails when there is an illegal method")
    void illegalMethodFound(VertxTestContext testContext) {
        var options = simpleCodeOptions();
        options.setIllegalMethods(Collections.singletonList("System.exit(.*)"));
        options.setCode(options.getCode() + "System.exit(0);");
        vertx.eventBus().<Response>request(
                VerticleAddresses.CODE_RUNNER_CONSUMER.toString(),
                options,
                messageReply -> testContext.verify(() -> {
                    assertTrue(messageReply.failed());
                    assertEquals("Please remove the following illegal methods:\n[System.exit(0);]",
                            messageReply.cause().getMessage());
                    testContext.completeNow();
                })
        );
    }

    @Test
    @DisplayName("Request fails when class name is different from classname in code")
    void differentClassNameFails(VertxTestContext testContext) {
        var options = simpleCodeOptions();
        options.setClassName("SomethingElse");
        vertx.eventBus().<Response>request(
                VerticleAddresses.CODE_RUNNER_CONSUMER.toString(),
                options,
                messageReply -> testContext.verify(() -> {
                    assertTrue(messageReply.failed());
                    assertTrue(messageReply.cause()
                            .getMessage()
                            .contains("class ScalesProblem is public, " +
                                    "should be declared in a file named ScalesProblem.java"));
                    testContext.completeNow();
                })
        );
    }

    private CodeOptions simpleCodeOptions() {
        List<Double> weights = List.of(
                1.0,
                2.0,
                3.0,
                4.0,
                5.0
        );
        return CodeOptions.builder()
                .className("ScalesProblem")
                .packageName("com.exercise")
                .methodToCall("runScales")
                .iterations(5)
                .importsAllowed(List.of("import java.util.List;", "import java.util.ArrayList;"))
                .illegalMethods(Collections.emptyList())
                .weights(weights)
                .code("import java.util.ArrayList;\n" +
                        "import java.util.List;\n\n" +
                        "public class ScalesProblem {\n" +
                        "    private List<String> solutions = new ArrayList<>();\n\n" +
                        "    public String runScales(List<Double> weights, int iterations) {\n" +
                        "        return \"11011\";\n" +
                        "    }\n\n" +
                        "}")
                .build();
    }

    private CodeRunnerSummary simpleCodeSummary() {
        var summary = new CodeRunnerSummary();
        summary.setIterations(5);
        summary.setTimeRun(0.0);
        summary.setFitness(9.0);
        summary.setEfficacy(-800.0);
        return summary;
    }

    @AfterAll
    public static void cleanup() {
        vertx.close();
    }

}