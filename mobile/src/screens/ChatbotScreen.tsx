import React, {useState} from 'react';
import {
  ActivityIndicator,
  Button,
  FlatList,
  Modal,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';

import {askChatbot, getChatbotUsage} from '../services/chatbotApi';

type Message = {id: string; role: 'user' | 'assistant'; content: string};

export function ChatbotScreen(): React.JSX.Element {
  const queryClient = useQueryClient();
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [showLimitModal, setShowLimitModal] = useState(false);

  const usageQuery = useQuery({queryKey: ['chatbot', 'usage'], queryFn: getChatbotUsage});

  const askMutation = useMutation({
    mutationFn: askChatbot,
    onSuccess: data => {
      setMessages(prev => [
        ...prev,
        {id: `${Date.now()}-assistant`, role: 'assistant', content: data.answer},
      ]);

      if ((data.answer as string).toLowerCase().includes('gunluk limit doldu')) {
        setShowLimitModal(true);
      }

      void queryClient.invalidateQueries({queryKey: ['chatbot', 'usage']});
    },
  });

  const send = () => {
    const question = input.trim();
    if (!question) {
      return;
    }

    setMessages(prev => [...prev, {id: `${Date.now()}-user`, role: 'user', content: question}]);
    setInput('');
    askMutation.mutate(question);
  };

  return (
    <View style={styles.container}>
      <Text style={styles.header}>Chatbot</Text>
      {usageQuery.isLoading ? (
        <ActivityIndicator />
      ) : (
        <Text style={styles.usage}>
          Kalan: {usageQuery.data?.remaining_messages_today ?? '-'} / {usageQuery.data?.daily_limit ?? '-'}
        </Text>
      )}

      <FlatList
        data={messages}
        keyExtractor={item => item.id}
        renderItem={({item}) => (
          <View style={[styles.message, item.role === 'user' ? styles.user : styles.assistant]}>
            <Text>{item.content}</Text>
          </View>
        )}
        ListEmptyComponent={<Text style={styles.empty}>Sohbet baslatmak icin soru yazin.</Text>}
      />

      {askMutation.isPending ? <Text>Yanit bekleniyor...</Text> : null}

      <View style={styles.inputRow}>
        <TextInput
          style={styles.input}
          placeholder="Hava ile ilgili bir soru sor..."
          value={input}
          onChangeText={setInput}
        />
        <Button title="Gonder" onPress={send} />
      </View>

      <Modal visible={showLimitModal} transparent animationType="fade">
        <View style={styles.modalBackdrop}>
          <View style={styles.modalBody}>
            <Text style={styles.modalTitle}>Gunluk limit doldu</Text>
            <Text>Premium ile daha fazla chatbot kullanimina gecebilirsin.</Text>
            <Button title="Kapat" onPress={() => setShowLimitModal(false)} />
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, padding: 16, gap: 8},
  header: {fontSize: 20, fontWeight: '700'},
  usage: {color: '#4b5563'},
  empty: {textAlign: 'center', marginVertical: 20, color: '#6b7280'},
  message: {padding: 10, borderRadius: 8, marginVertical: 4},
  user: {alignSelf: 'flex-end', backgroundColor: '#dbeafe'},
  assistant: {alignSelf: 'flex-start', backgroundColor: '#e5e7eb'},
  inputRow: {flexDirection: 'row', alignItems: 'center', gap: 8},
  input: {flex: 1, borderWidth: 1, borderColor: '#d1d5db', borderRadius: 8, padding: 10},
  modalBackdrop: {flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', alignItems: 'center', justifyContent: 'center'},
  modalBody: {backgroundColor: '#fff', padding: 16, borderRadius: 12, width: '85%', gap: 10},
  modalTitle: {fontWeight: '700', fontSize: 16},
});
