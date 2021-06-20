package com.dercio.algonated_scales_service.runner.calculator;

import java.util.List;

public interface Calculator<T> {

    double calculate(T data, List<Integer> solution);
}
