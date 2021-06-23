package com.dercio.algonated_scales_service.runner.demo;

import com.dercio.algonated_scales_service.algorithms.Algorithm;
import com.dercio.algonated_scales_service.algorithms.Solution;
import com.dercio.algonated_scales_service.response.Response;
import com.dercio.algonated_scales_service.runner.CodeRunnerSummary;
import com.dercio.algonated_scales_service.runner.calculator.ScalesEfficiencyCalculator;
import com.dercio.algonated_scales_service.runner.calculator.ScalesFitnessCalculator;
import com.google.common.base.Stopwatch;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ScalesRunner {

    private final DemoRequest<List<Double>> request;
    private final CodeRunnerSummary summary = new CodeRunnerSummary();
    private List<Integer> solution = Collections.emptyList();
    private List<List<Integer>> solutions = Collections.emptyList();

    public ScalesRunner(DemoRequest<List<Double>> request) {
        this.request = request;
    }

    public ScalesRunner run() {
        Algorithm<Solution, List<Double>> algorithm = Algorithm.getScalesAlgorithm(request);

        Stopwatch timer = Stopwatch.createStarted();
        solution = algorithm.run(request.getWeights(), request.getIterations()).getSolution();
        timer.stop();

        solutions = algorithm.getSolutions();

        buildSummary(timer.elapsed(TimeUnit.MILLISECONDS));
        return this;
    }

    private void buildSummary(long elapsed) {
        summary.setTimeRun(elapsed);
        summary.setIterations(request.getIterations());
        summary.setFitness(new ScalesFitnessCalculator().calculate(request.getWeights(), solution));
        summary.setEfficacy(new ScalesEfficiencyCalculator().calculate(request.getWeights(), solution));
    }

    public Response toResponse() {
        return new Response()
                .setSuccess(true)
                .setConsoleOutput("Compile and Run was a success!")
                .setResult(solution)
                .setData(request.getWeights())
                .setSummary(summary)
                .setSolutions(solutions);
    }
}
