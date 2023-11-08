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

class Thread {
  id = '';
  createdAt: Date | undefined;
  metadata: any;
}

class Content {
  type = '';
  text: TextContent | null = null;
}

class TextContent {
  value = '';
  annotations: [] | null = null;
}

class ThreadMessage {
  id = '';
  created_at: Date | null = null;
  thread_id: string | null = null;
  file_ids: [] | null = null;
  assistant_id: string | null = null;
  run_id: string | null = null;
  role: 'user' | 'assistant' | undefined;
  content: Array<Content> | null = null;
  metadata: any;
}

export default Thread;
export {ThreadMessage, Content, TextContent};
