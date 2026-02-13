import {apiClient} from './apiClient';

export async function askChatbot(question: string) {
  const response = await apiClient.post('/chatbot/ask', {question});
  return response.data;
}

export async function getChatbotUsage() {
  const response = await apiClient.get('/chatbot/usage');
  return response.data;
}
