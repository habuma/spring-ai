/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.stabilityai;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.image.ImageResponseMetadata;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.stabilityai.api.StabilityAiApi;
import org.springframework.ai.stabilityai.api.StabilityAiImageOptions;
import org.springframework.util.Assert;

/**
 * StabilityAiImageModel is a class that implements the ImageModel interface. It provides
 * a client for calling the StabilityAI image generation API.
 */
public class StabilityAiImageModel implements ImageModel {

	private final StabilityAiImageOptions defaultOptions;

	private final StabilityAiApi stabilityAiApi;

	public StabilityAiImageModel(StabilityAiApi stabilityAiApi) {
		this(stabilityAiApi, StabilityAiImageOptions.builder().build());
	}

	public StabilityAiImageModel(StabilityAiApi stabilityAiApi, StabilityAiImageOptions defaultOptions) {
		Assert.notNull(stabilityAiApi, "StabilityAiApi must not be null");
		Assert.notNull(defaultOptions, "StabilityAiImageOptions must not be null");
		this.stabilityAiApi = stabilityAiApi;
		this.defaultOptions = defaultOptions;
	}

	private static StabilityAiApi.GenerateImageRequest getGenerateImageRequest(ImagePrompt stabilityAiImagePrompt,
			StabilityAiImageOptions optionsToUse) {
		return new StabilityAiApi.GenerateImageRequest.Builder()
			.withTextPrompts(stabilityAiImagePrompt.getInstructions()
				.stream()
				.map(message -> new StabilityAiApi.GenerateImageRequest.TextPrompts(message.getText(),
						message.getWeight()))
				.collect(Collectors.toList()))
			.withHeight(optionsToUse.getHeight())
			.withWidth(optionsToUse.getWidth())
			.withCfgScale(optionsToUse.getCfgScale())
			.withClipGuidancePreset(optionsToUse.getClipGuidancePreset())
			.withSampler(optionsToUse.getSampler())
			.withSamples(optionsToUse.getN())
			.withSeed(optionsToUse.getSeed())
			.withSteps(optionsToUse.getSteps())
			.withStylePreset(optionsToUse.getStylePreset())
			.build();
	}

	public StabilityAiImageOptions getOptions() {
		return this.defaultOptions;
	}

	/**
	 * Calls the StabilityAiImageModel with the given StabilityAiImagePrompt and returns
	 * the ImageResponse. This overloaded call method lets you pass the full set of Prompt
	 * instructions that StabilityAI supports.
	 * @param imagePrompt the StabilityAiImagePrompt containing the prompt and image model
	 * options
	 * @return the ImageResponse generated by the StabilityAiImageModel
	 */
	public ImageResponse call(ImagePrompt imagePrompt) {
		// Merge the runtime options passed via the prompt with the default options
		// configured via the constructor.
		// Runtime options overwrite StabilityAiImageModel options
		StabilityAiImageOptions runtimeOptions = (StabilityAiImageOptions) imagePrompt.getOptions();
		StabilityAiImageOptions requestImageOptions = mergeOptions(runtimeOptions, this.defaultOptions);

		// Copy the org.springframework.ai.model derived ImagePrompt and ImageOptions data
		// types to the data types used in StabilityAiApi
		StabilityAiApi.GenerateImageRequest generateImageRequest = getGenerateImageRequest(imagePrompt,
				requestImageOptions);

		// Make the request
		StabilityAiApi.GenerateImageResponse generateImageResponse = this.stabilityAiApi
			.generateImage(generateImageRequest);

		// Convert to org.springframework.ai.model derived ImageResponse data type
		return convertResponse(generateImageResponse);
	}

	private ImageResponse convertResponse(StabilityAiApi.GenerateImageResponse generateImageResponse) {
		List<ImageGeneration> imageGenerationList = generateImageResponse.artifacts()
			.stream()
			.map(entry -> new ImageGeneration(new Image(null, entry.base64()),
					new StabilityAiImageGenerationMetadata(entry.finishReason(), entry.seed())))
			.toList();

		return new ImageResponse(imageGenerationList, new ImageResponseMetadata());
	}

	/**
	 * Merge runtime and default {@link ImageOptions} to compute the final options to use
	 * in the request.
	 */
	private StabilityAiImageOptions mergeOptions(StabilityAiImageOptions runtimeOptions,
			StabilityAiImageOptions defaultOptions) {
		if (runtimeOptions == null) {
			return defaultOptions;
		}

		return StabilityAiImageOptions.builder()
			// Handle portable image options
			.withModel(ModelOptionsUtils.mergeOption(runtimeOptions.getModel(), defaultOptions.getModel()))
			.withN(ModelOptionsUtils.mergeOption(runtimeOptions.getN(), defaultOptions.getN()))
			.withResponseFormat(ModelOptionsUtils.mergeOption(runtimeOptions.getResponseFormat(),
					defaultOptions.getResponseFormat()))
			.withWidth(ModelOptionsUtils.mergeOption(runtimeOptions.getWidth(), defaultOptions.getWidth()))
			.withHeight(ModelOptionsUtils.mergeOption(runtimeOptions.getHeight(), defaultOptions.getHeight()))
			.withStylePreset(ModelOptionsUtils.mergeOption(runtimeOptions.getStyle(), defaultOptions.getStyle()))
			// Handle Stability AI specific image options
			.withCfgScale(defaultOptions.getCfgScale())
			.withClipGuidancePreset(defaultOptions.getClipGuidancePreset())
			.withSampler(runtimeOptions.getSampler())
			.withSeed(defaultOptions.getSeed())
			.withSteps(runtimeOptions.getSteps())
			.withStylePreset(runtimeOptions.getStylePreset())
			.build();
	}

}
