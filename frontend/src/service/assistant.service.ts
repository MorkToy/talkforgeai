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

import axios from 'axios';
import Run from '@/store/to/run';
import Thread, {ThreadMessage} from '@/store/to/thread';

class AssistantService {

  async retrieveAssistant(assistantId: string) {
    console.log('Retrieving assistant with id:', assistantId);
    try {
      const result = await axios.get(`/api/v1/assistants/${assistantId}`);
      return result.data;
    } catch (error) {
      throw new Error('Error reading assistant data: ' + error);
    }
  }

  async createThread() {
    try {
      const result = await axios.post(`/api/v1/threads`);
      return result.data;
    } catch (error) {
      throw new Error('Error creating thread: ' + error);
    }
  }

  async submitUserMessage(threadId: string, content: string) {
    try {
      const result = await axios.post(
        `/api/v1/threads/${threadId}/messages`,
        {content, role: 'user'},
      );
      return result.data;
    } catch (error) {
      throw new Error('Error submitting message: ' + error);
    }
  }

  async runConversation(threadId: string, assistantId: string): Promise<Run> {
    try {
      const result = await axios.post(`/api/v1/threads/${threadId}/runs`, {assistant_id: assistantId});
      return result.data;
    } catch (error) {
      throw new Error('Error submitting message: ' + error);
    }
  }

  async retrieveRun(threadId: string, runId: string): Promise<Run> {
    try {
      const result = await axios.get(`/api/v1/threads/${threadId}/runs/${runId}`);
      return result.data;
    } catch (error) {
      throw new Error('Error retrieving run: ' + error);
    }
  }

  async retrieveLastAssistentMessage(threadId: string): Promise<ThreadMessage | undefined> {
    try {
      const result = await axios.get(
        `/api/v1/threads/${threadId}/messages`,
        {
          params: {
            limit: 1,
            order: 'desc',
          },
        },
      );
      const response = result.data;

      if (response.data && response.data.length > 0) {
        return response.data[0];
      }
    } catch (error) {
      throw new Error('Error retrieving messages: ' + error);
    }
  }

  async retrieveMessages(threadId: string): Promise<Array<ThreadMessage>> {
    try {
      const result = await axios.get(
        `/api/v1/threads/${threadId}/messages`,
        {
          params: {
            order: 'asc',
          },
        },
      );
      return result.data.data;
    } catch (error) {
      throw new Error('Error retrieving messages: ' + error);
    }
  }

  async retrieveThreads(): Promise<Array<Thread>> {
    const result = await axios.get(
      `/api/v1/threads`,
      {
        params: {},
      },
    );

    return result.data;
  }
}

export default AssistantService;
