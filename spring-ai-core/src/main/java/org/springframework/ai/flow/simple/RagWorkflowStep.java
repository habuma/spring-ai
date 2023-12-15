package org.springframework.ai.flow.simple;

import org.springframework.ai.client.AiClient;
import org.springframework.ai.client.AiResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.memory.Memory;
import org.springframework.ai.prompt.Prompt;
import org.springframework.ai.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class RagWorkflowStep implements Function<String, AiResponse> {

	private static final String DEFAULT_PROMPT_TEMPLATE = """
			You are a helpful assistant, conversing with a user about the subjects contained in a set of documents.
			Use the information from the DOCUMENTS section to provide accurate answers. If unsure, simply state
			that you don't know the answer.
			QUESTION:
			{input}
			DOCUMENTS:
			{documents}""";

	private final AiClient aiClient;

	private final VectorStore vectorStore;

	private final Memory memory;

	private final String promptTemplateString;

	public RagWorkflowStep(AiClient aiClient, VectorStore vectorStore, Memory memory) {
		this(aiClient, vectorStore, memory, DEFAULT_PROMPT_TEMPLATE);
	}

	public RagWorkflowStep(AiClient aiClient, VectorStore vectorStore, Memory memory, String promptTemplate) {
		this.aiClient = aiClient;
		this.vectorStore = vectorStore;
		this.memory = memory;
		this.promptTemplateString = promptTemplate;
	}

	public AiResponse apply(String inputQuestion) {
		List<Document> documents = vectorStore.similaritySearch(inputQuestion); // TODO:
																				// Would
																				// probably
																				// use
																				// SearchRequest
																				// instead
																				// of it
																				// were
																				// available
																				// in this
																				// branch
		List<String> contentList = documents.stream().map(doc -> {
			return doc.getContent() + "\n";
		}).toList();
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("input", inputQuestion);
		parameters.put("documents", contentList);
		PromptTemplate promptTemplate = new PromptTemplate(promptTemplateString);
		String prompt = promptTemplate.render(parameters).trim();
		AiResponse aiResponse = aiClient.generate(new Prompt(prompt));

		if (memory != null) {
			memory.save(Collections.emptyMap(), Map.of("response", aiResponse.getGenerations().get(0).getText())); // maybe?
		}

		return aiResponse;
	}

}