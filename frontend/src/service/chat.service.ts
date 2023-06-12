import axios from 'axios';
import Session from '@/store/to/session';

class ChatService {
  async readSessionEntries(): Promise<Array<Session>> {
    try {
      const result = await axios.get('/api/v1/chat/session');
      return result.data;
    } catch (error) {
      throw new Error('Error reading index entries: ' + error);
    }
  }

  async readSessionEntry(sessionId: string): Promise<Session> {
    try {
      const result = await axios.get(`/api/v1/chat/session/${sessionId}`);
      return result.data;
    } catch (error) {
      throw new Error('Error reading session entry:  ' + error);
    }
  }

  async createNewSession(personaId: string) {
    console.log('Creating new session with personaId:', personaId);
    try {
      const result = await axios.post('/api/v1/chat/create', {personaId});
      return result.data;
    } catch (error) {
      throw new Error('Error creating chat session: ' + error);
    }
  }

  async getLastResult(sessionId: string) {
    try {
      const result = await axios.get(
        `/api/v1/chat/result/${sessionId}`,
        {
          timeout: 50000,
          headers: {
            'Content-Type': 'application/json',
          },
        });
      return result.data;
    } catch (error) {
      throw new Error('Error submitting prompt: ' + error);
    }
  }

  async submit(sessionId: string, prompt: string) {
    try {
      const result = await axios.post(
        `/api/v1/chat/submit`,
        {sessionId, prompt},
        {
          timeout: 50000,
          headers: {
            'Content-Type': 'application/json',
          },
        });
      return result.data;
    } catch (error) {
      throw new Error('Error submitting prompt: ' + error);
    }
  }
}

export default ChatService;
