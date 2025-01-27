/*
 * Copyright (c) 2023 Jean Schmitz.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.talkforgeai.service.openai.image.dto;

public record OpenAIImageRequest(String prompt, int n, String size, String model) {

  public OpenAIImageRequest(String prompt, int n, String size) {
    this(prompt, n, size, "dall-e-3");
  }

  public OpenAIImageRequest(String prompt) {
    this(prompt, 1, "1024x1024", "dall-e-3");
  }
}
