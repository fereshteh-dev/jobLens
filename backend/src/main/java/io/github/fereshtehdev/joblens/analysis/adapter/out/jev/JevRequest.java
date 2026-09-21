package io.github.fereshtehdev.joblens.analysis.adapter.out.jev;

import java.util.Map;

/** Request body for {@code POST /v1/systemone}. {@code questions} values are plain maps - see JevPostingClassifier.buildQuestions(). */
record JevRequest(String state, String model, Map<String, Object> questions) {
}
