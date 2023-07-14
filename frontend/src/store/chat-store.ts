import {defineStore} from 'pinia';
import Session from '@/store/to/session';
import ChatMessage from '@/store/to/chat-message';
import Persona from '@/store/to/persona';
import ChatService from '@/service/chat.service';
import PersonaService from '@/service/persona.service';
import Role from '@/store/to/role';
import ChatStreamService from '@/service/chat-stream.service';
import HighlightingService from '@/service/highlighting.service';

const chatService = new ChatService();
const chatStreamService = new ChatStreamService();
const personaService = new PersonaService();
const highlightingService = new HighlightingService();

export const useChatStore = defineStore('chat', {
  state: () => {
    return {
      sessionId: '',
      messages: [] as Array<ChatMessage>,
      selectedPersona: {} as Persona,
      personaList: [] as Array<Persona>,
      chat: {
        configHeaderEnabled: true,
        autoSpeak: false,
      },
      currentStatusMessage: '',
      sessions: [] as Array<Session>,
      selectedSessionId: null,
    };
  },
  getters: {
    autoSpeak(): boolean {
      return this.chat.autoSpeak;
    },
    isEmptySession(): boolean {
      return this.messages.length == 0;
    },
    maxMessageIndex(): number {
      return this.messages.length - 1;
    },
  },
  actions: {
    async newSession() {
      this.sessionId = '';
      this.messages = [];
      this.chat.configHeaderEnabled = true;
    },
    disableConfigHeader() {
      console.log('disableConfigHeader');
      this.chat.configHeaderEnabled = false;
    },
    async loadIndex() {
      this.sessions = await chatService.readSessionEntries();
    },
    async getLastResult() {
      return await chatService.getLastResult(this.sessionId);
    },
    async submitPrompt(prompt: string) {
      this.chat.configHeaderEnabled = false;

      if (!this.sessionId || this.sessionId === '') {
        this.sessionId = await chatService.createNewSession(this.selectedPersona.personaId);
      }

      this.messages.push(new ChatMessage(Role.USER, prompt));

      console.log('Submitting prompt', this.sessionId, prompt);
      const result = await chatService.submit(this.sessionId, prompt);
      console.log('Result Message.', result);
      this.messages = [...this.messages, ...result.processedMessages];

      const message = result.processedMessages[result.processedMessages.length - 1];
      if (message.function_call && message.role === Role.ASSISTANT) {
        const result = await chatService.submitFunctionConfirm(this.sessionId);

        console.log('Result Message after Function call.', result);
        this.messages = [...this.messages, ...result.processedMessages];
      }

      //console.log('Prompt submitted');

    },
    async streamPrompt(prompt: string) {
      this.chat.configHeaderEnabled = false;

      if (!this.sessionId || this.sessionId === '') {
        this.sessionId = await chatService.createNewSession(this.selectedPersona.personaId);
      }

      this.messages.push(new ChatMessage(Role.USER, prompt));

      console.log('Submitting prompt', this.sessionId, prompt);

      const lastMessage = this.messages[this.messages.length - 1];

      await chatStreamService.streamSubmit(this.sessionId, prompt, (content, isDone) => {
        content.forEach(c => {
          lastMessage.content += c;
        });
      });

    },
    async loadChatSession(sessionId: string) {
      const chatSession = await chatService.readSessionEntry(sessionId);

      highlightingService.highlightCodeInChatMessage(chatSession.chatMessages);

      this.$patch({
        sessionId: chatSession.id,
        messages: chatSession.chatMessages,
        selectedPersona: chatSession.persona,
        chat: {configHeaderEnabled: false},
      });

    },
    async readPersona() {
      this.personaList = await personaService.readPersona();
      this.selectedPersona = this.personaList[0];
    },
    toggleAutoSpeak() {
      this.chat.autoSpeak = !this.chat.autoSpeak;
    },
    updateStatus(sessionId: string, message: string) {
      if (sessionId === this.sessionId) {
        this.currentStatusMessage = message;
      }
    },
  },

});
