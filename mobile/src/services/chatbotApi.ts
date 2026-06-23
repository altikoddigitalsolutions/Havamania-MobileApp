import {apiClient} from './apiClient';

export async function askChatbot(question: string, tone?: string) {
  const response = await apiClient.post('/chatbot/ask', {question, tone});
  return response.data;
}

export async function getChatbotUsage() {
  const response = await apiClient.get('/chatbot/usage');
  return response.data;
}
