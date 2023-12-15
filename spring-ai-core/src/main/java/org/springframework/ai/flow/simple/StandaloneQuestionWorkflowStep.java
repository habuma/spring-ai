package org.springframework.ai.flow.simple;

import org.springframework.ai.client.AiClient;
import org.springframework.ai.client.AiResponse;
import org.springframework.ai.memory.Memory;
import org.springframework.ai.prompt.Prompt;
import org.springframework.ai.prompt.PromptTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class StandaloneQuestionWorkflowStep implements Function<String, AiResponse> {

	public static final String DEFAULT_PROMPT_TEMPLATE = """
			Given the following conversation and a follow up question, rephrase the follow up question to be a standalone
			question, in its original language.
			Chat History:
			{history}
			Follow Up Input: {input}
			Standalone question:""";

	private final AiClient aiClient;

	private final Memory memory;

	private final String promptTemplateString;

	public StandaloneQuestionWorkflowStep(AiClient aiClient, Memory memory) {
		this(aiClient, memory, DEFAULT_PROMPT_TEMPLATE);
	}

	public StandaloneQuestionWorkflowStep(AiClient aiClient, Memory memory, String promptTemplate) {
		this.aiClient = aiClient;
		this.memory = memory;
		this.promptTemplateString = promptTemplate;
	}

	@Override
	public AiResponse apply(String inputQuestion) {
		Map<String, Object> parameters = new HashMap<>();
		Map<String, Object> memoryRecalled = memory.load(Collections.emptyMap()); // TODO:
																					// assumes
																					// a
																					// single-entry
																					// named
																					// "history"
		parameters.putAll(memoryRecalled);
		parameters.put("input", inputQuestion);
		PromptTemplate promptTemplate = new PromptTemplate(promptTemplateString);
		String prompt = promptTemplate.render(parameters);
		AiResponse aiResponse = aiClient.generate(new Prompt(prompt));
		memory.save(Map.of("question", inputQuestion), Collections.emptyMap()); // maybe?
		return aiResponse;
	}

}
