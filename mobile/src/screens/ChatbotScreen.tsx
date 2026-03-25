import React, {useEffect, useRef, useState} from 'react';
import {
  ActivityIndicator,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import {useMutation, useQuery} from '@tanstack/react-query';
import {useNavigation} from '@react-navigation/native';

import {askChatbot} from '../services/chatbotApi';
import {getCurrentWeather} from '../services/weatherApi';
import {DarkColors, FontSize, LightColors, Radius, Spacing, getWeatherEmoji, getWeatherLabel} from '../theme';
import {useThemeStore} from '../store/themeStore';
import {useAuthStore} from '../store/authStore';

// ── Tip ──────────────────────────────────────────────────────────────────────
type Message = {
  id: string;
  role: 'user' | 'assistant' | 'weather-card';
  content: string;
  weatherData?: any;
};

// ── Varsayılan Konum ─────────────────────────────────────────────────────────
const DEFAULT_LAT = 41.0082;
const DEFAULT_LON = 28.9784;
const DEFAULT_CITY = 'İstanbul, TR';

// ── Hızlı Chip'ler ───────────────────────────────────────────────────────────
const QUICK_CHIPS = [
  'What should I wear?',
  'UV Index report',
  'Weekend forecast',
  'Is it good for hiking?',
];

export function ChatbotScreen(): React.JSX.Element {
  const navigation = useNavigation();
  const {theme} = useThemeStore();
  const {isGuest} = useAuthStore();
  const C = theme === 'dark' ? DarkColors : LightColors;
  const flatRef = useRef<FlatList>(null);

  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [initialized, setInitialized] = useState(false);

  // Mevcut hava durumunu çek
  const weatherQuery = useQuery({
    queryKey: ['weather', 'current', DEFAULT_LAT, DEFAULT_LON],
    queryFn: () => getCurrentWeather(DEFAULT_LAT, DEFAULT_LON),
    staleTime: 5 * 60 * 1000,
  });

  // Karşılama mesajını ekle
  useEffect(() => {
    if (!initialized && weatherQuery.data) {
      const wd = weatherQuery.data;
      const greeting: Message = {
        id: 'welcome',
        role: 'assistant',
        content: `Hello! I'm Havamania. I've checked the forecast for ${DEFAULT_CITY} — it's ${getWeatherLabel(wd.weather_code).toLowerCase()} at ${wd.temperature}°C. How can I help you plan your day?`,
      };
      setMessages([greeting]);
      setInitialized(true);
    }
  }, [weatherQuery.data, initialized]);

  // Chatbot mutation
  const askMutation = useMutation({
    mutationFn: (question: string) => {
      if (isGuest) {
        // Misafir modunda yerel yanıt üret
        return Promise.resolve(buildLocalAnswer(question, weatherQuery.data));
      }
      return askChatbot(question);
    },
    onSuccess: (data: any, question: string) => {
      const answer = data?.answer ?? data?.content ?? String(data);
      const isWeatherQ = /weather|forecast|temp|rain|sun|wind|uv/i.test(question);

      setMessages(prev => {
        const next = [...prev];
        // "typing" mesajını kaldır
        const typingIdx = next.findIndex(m => m.id === 'typing');
        if (typingIdx >= 0) next.splice(typingIdx, 1);

        next.push({
          id: `${Date.now()}-assistant`,
          role: 'assistant',
          content: answer,
        });

        // Hava durumu sorusuysa kart ekle
        if (isWeatherQ && weatherQuery.data) {
          next.push({
            id: `${Date.now()}-card`,
            role: 'weather-card',
            content: '',
            weatherData: weatherQuery.data,
          });
        }
        return next;
      });
    },
    onError: () => {
      setMessages(prev => {
        const next = prev.filter(m => m.id !== 'typing');
        return [
          ...next,
          {
            id: `${Date.now()}-err`,
            role: 'assistant',
            content:
              'Şu an bağlanamıyorum. Lütfen backend\'in çalıştığından emin ol veya misafir modunu kullan.',
          },
        ];
      });
    },
  });

  const send = (text?: string) => {
    const question = (text ?? input).trim();
    if (!question || askMutation.isPending) return;

    setMessages(prev => [
      ...prev,
      {id: `${Date.now()}-user`, role: 'user', content: question},
      {id: 'typing', role: 'assistant', content: '...'},
    ]);
    setInput('');
    askMutation.mutate(question);
  };

  // FlatList'i otomatik kaydır
  useEffect(() => {
    if (messages.length > 0) {
      setTimeout(() => flatRef.current?.scrollToEnd({animated: true}), 100);
    }
  }, [messages]);

  const s = makeStyles(C);

  return (
    <SafeAreaView style={s.safe}>
      <StatusBar
        barStyle={theme === 'dark' ? 'light-content' : 'dark-content'}
        backgroundColor={C.bg}
      />

      {/* ── Header ── */}
      <View style={s.header}>
        <TouchableOpacity
          onPress={() => navigation.canGoBack() && navigation.goBack()}
          style={s.backBtn}>
          <Text style={s.backArrow}>‹</Text>
        </TouchableOpacity>
        <View style={s.headerCenter}>
          <Text style={s.headerTitle}>Havamania AI</Text>
          <View style={s.onlineRow}>
            <View style={s.onlineDot} />
            <Text style={s.onlineText}>ONLINE</Text>
          </View>
        </View>
        <TouchableOpacity style={s.infoBtn}>
          <Text style={s.infoBtnText}>ⓘ</Text>
        </TouchableOpacity>
      </View>

      <KeyboardAvoidingView
        style={{flex: 1}}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 0}>

        {/* ── Mesajlar ── */}
        <FlatList
          ref={flatRef}
          data={messages}
          keyExtractor={item => item.id}
          contentContainerStyle={s.messageList}
          showsVerticalScrollIndicator={false}
          renderItem={({item}) => {
            if (item.role === 'weather-card' && item.weatherData) {
              return <WeatherCard data={item.weatherData} C={C} city={DEFAULT_CITY} />;
            }
            return (
              <MessageBubble
                message={item}
                C={C}
                isTyping={item.id === 'typing'}
              />
            );
          }}
          ListEmptyComponent={
            weatherQuery.isLoading ? (
              <View style={s.emptyState}>
                <ActivityIndicator color={C.accent} />
              </View>
            ) : null
          }
        />

        {/* ── Hızlı Chip'ler ── */}
        <View>
          <FlatList
            horizontal
            data={QUICK_CHIPS}
            keyExtractor={item => item}
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={s.chipsContainer}
            renderItem={({item}) => (
              <TouchableOpacity
                style={s.chip}
                onPress={() => send(item)}
                disabled={askMutation.isPending}>
                <Text style={s.chipText}>{item}</Text>
              </TouchableOpacity>
            )}
          />
        </View>

        {/* ── Input Bar ── */}
        <View style={s.inputBar}>
          <TouchableOpacity style={s.addBtn}>
            <Text style={s.addBtnText}>+</Text>
          </TouchableOpacity>
          <TextInput
            style={s.input}
            placeholder="Ask about the weather..."
            placeholderTextColor={C.textMuted}
            value={input}
            onChangeText={setInput}
            onSubmitEditing={() => send()}
            returnKeyType="send"
            multiline={false}
          />
          <TouchableOpacity
            style={[s.sendBtn, (!input.trim() || askMutation.isPending) && s.sendBtnDisabled]}
            onPress={() => send()}
            disabled={!input.trim() || askMutation.isPending}>
            <Text style={s.sendBtnIcon}>▶</Text>
          </TouchableOpacity>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

// ── MessageBubble ─────────────────────────────────────────────────────────────
function MessageBubble({
  message,
  C,
  isTyping,
}: {
  message: Message;
  C: typeof DarkColors;
  isTyping?: boolean;
}) {
  const isUser = message.role === 'user';

  return (
    <View style={[bubbleStyles(C).wrapper, isUser ? bubbleStyles(C).wrapperUser : bubbleStyles(C).wrapperBot]}>
      {!isUser && (
        <View style={bubbleStyles(C).avatar}>
          <Text style={{fontSize: 18}}>☁️</Text>
        </View>
      )}
      <View style={[bubbleStyles(C).bubble, isUser ? bubbleStyles(C).bubbleUser : bubbleStyles(C).bubbleBot]}>
        {!isUser && !isTyping && (
          <Text style={bubbleStyles(C).senderLabel}>HAVAMANIA AI</Text>
        )}
        {isTyping ? (
          <ActivityIndicator size="small" color={C.textSecondary} />
        ) : (
          <Text style={[bubbleStyles(C).text, isUser && bubbleStyles(C).textUser]}>
            {message.content}
          </Text>
        )}
      </View>
      {isUser && (
        <View style={bubbleStyles(C).userAvatar}>
          <Text style={{fontSize: 16}}>👤</Text>
        </View>
      )}
    </View>
  );
}

const bubbleStyles = (C: typeof DarkColors) =>
  StyleSheet.create({
    wrapper: {flexDirection: 'row', marginBottom: Spacing.md, alignItems: 'flex-end', gap: Spacing.sm},
    wrapperBot: {paddingLeft: Spacing.md},
    wrapperUser: {paddingRight: Spacing.md, flexDirection: 'row-reverse'},
    avatar: {
      width: 36,
      height: 36,
      borderRadius: 18,
      backgroundColor: C.bgCard,
      justifyContent: 'center',
      alignItems: 'center',
      borderWidth: 1,
      borderColor: C.border,
    },
    userAvatar: {
      width: 36,
      height: 36,
      borderRadius: 18,
      backgroundColor: C.bgCard,
      justifyContent: 'center',
      alignItems: 'center',
    },
    bubble: {
      maxWidth: '75%',
      borderRadius: Radius.lg,
      padding: Spacing.md,
    },
    bubbleBot: {
      backgroundColor: C.bgCard,
      borderBottomLeftRadius: 4,
    },
    bubbleUser: {
      backgroundColor: C.accentBtn,
      borderBottomRightRadius: 4,
    },
    senderLabel: {
      fontSize: FontSize.xs,
      fontWeight: '800',
      color: C.textMuted,
      letterSpacing: 0.8,
      marginBottom: 4,
    },
    text: {
      fontSize: FontSize.md,
      color: C.text,
      lineHeight: 22,
    },
    textUser: {color: '#FFFFFF'},
  });

// ── WeatherCard ──────────────────────────────────────────────────────────────
function WeatherCard({
  data,
  C,
  city,
}: {
  data: any;
  C: typeof DarkColors;
  city: string;
}) {
  return (
    <View style={[cardStyles(C).wrapper]}>
      <View style={cardStyles(C).card}>
        <View style={cardStyles(C).cardHeader}>
          <Text style={cardStyles(C).cardLabel}>CURRENT WEATHER</Text>
        </View>
        <View style={cardStyles(C).cardBody}>
          <View style={{flex: 1}}>
            <Text style={cardStyles(C).cardTemp}>{data.temperature}°C</Text>
            <Text style={cardStyles(C).cardDesc}>{getWeatherLabel(data.weather_code)}</Text>
            <Text style={cardStyles(C).cardCity}>{city}</Text>
          </View>
          <View style={cardStyles(C).cardIconBox}>
            <Text style={{fontSize: 36}}>{getWeatherEmoji(data.weather_code)}</Text>
          </View>
        </View>
        <View style={cardStyles(C).statsRow}>
          <StatItem label="Humidity" value={`${data.humidity}%`} C={C} />
          <StatItem label="Wind" value={`${data.wind_speed}km/h`} C={C} />
          <StatItem label="Feels" value={`${data.feels_like}°`} C={C} />
        </View>
      </View>
    </View>
  );
}

function StatItem({label, value, C}: {label: string; value: string; C: typeof DarkColors}) {
  return (
    <View style={{alignItems: 'center', flex: 1}}>
      <Text style={{fontSize: FontSize.xs, color: C.textMuted}}>{label}</Text>
      <Text style={{fontSize: FontSize.sm, fontWeight: '700', color: C.text}}>{value}</Text>
    </View>
  );
}

const cardStyles = (C: typeof DarkColors) =>
  StyleSheet.create({
    wrapper: {
      paddingHorizontal: Spacing.md,
      paddingLeft: 36 + Spacing.md + Spacing.sm + Spacing.md, // avatar + gaps + padding
      marginBottom: Spacing.md,
    },
    card: {
      backgroundColor: C.bgSecondary,
      borderRadius: Radius.lg,
      padding: Spacing.md,
      borderWidth: 1,
      borderColor: C.border,
    },
    cardHeader: {marginBottom: Spacing.xs},
    cardLabel: {
      fontSize: FontSize.xs,
      fontWeight: '800',
      color: C.textMuted,
      letterSpacing: 1,
    },
    cardBody: {
      flexDirection: 'row',
      alignItems: 'center',
      marginBottom: Spacing.sm,
    },
    cardTemp: {fontSize: FontSize.xxl + 8, fontWeight: '800', color: C.text},
    cardDesc: {fontSize: FontSize.md, color: C.accent, fontWeight: '600'},
    cardCity: {fontSize: FontSize.sm, color: C.textSecondary},
    cardIconBox: {
      width: 64,
      height: 64,
      borderRadius: Radius.md,
      backgroundColor: '#FF9800',
      justifyContent: 'center',
      alignItems: 'center',
    },
    statsRow: {
      flexDirection: 'row',
      paddingTop: Spacing.sm,
      borderTopWidth: 0.5,
      borderTopColor: C.divider,
    },
  });

// ── Yerel Chatbot (Misafir Modu) ─────────────────────────────────────────────
function buildLocalAnswer(question: string, weather: any): {answer: string} {
  if (!weather) return {answer: 'Hava durumu bilgisi yükleniyor, lütfen bekle.'};
  const q = question.toLowerCase();
  const temp = weather.temperature;
  const desc = getWeatherLabel(weather.weather_code);
  const wind = weather.wind_speed;
  const humidity = weather.humidity;

  if (/wear|giy|kıyafet/i.test(q)) {
    if (temp > 25) return {answer: `${temp}°C ile güneşli bir hava! Hafif giysiler ve güneş kremi öneririm. 😎`};
    if (temp > 15) return {answer: `${temp}°C ve ${desc.toLowerCase()}. Hafif bir ceket yeterli olur. 👕`};
    return {answer: `${temp}°C oldukça serin. Mont ve katmanlı giysiler giyin! 🧥`};
  }
  if (/hike|yürüyüş|outdoor|dışarı/i.test(q)) {
    if (wind < 20 && temp > 10 && weather.weather_code < 60) {
      return {answer: `${desc} ve ${wind} km/h rüzgar ile yürüyüş için harika bir gün! Güneş kremi unutma. 🥾`};
    }
    return {answer: `Bugün yürüyüş için ideal değil — ${desc.toLowerCase()} ve ${wind} km/h rüzgar var. Dikkatli ol!`};
  }
  if (/picnic|piknik/i.test(q)) {
    if (weather.weather_code < 3 && wind < 20) {
      return {answer: `Harika bir piknik günü! ${temp}°C, ${desc.toLowerCase()} ve hafif rüzgar. Güneş kremin yanında olsun. 🧺`};
    }
    return {answer: `Bugün piknik için biraz riskli — ${desc.toLowerCase()}. Yarın tekrar kontrol et!`};
  }
  if (/uv|güneş/i.test(q)) {
    const uv = weather.uv_index ?? 3;
    const level = uv <= 2 ? 'Düşük' : uv <= 5 ? 'Orta' : uv <= 7 ? 'Yüksek' : 'Çok Yüksek';
    return {answer: `UV Index şu an ${uv} — ${level}. ${uv > 3 ? '30+ SPF güneş kremi kullan! ☀️' : 'Güvenli bir seviye.'}`};
  }
  if (/weekend|hafta sonu/i.test(q)) {
    return {answer: `Hafta sonu tahminine bakmak için ana sayfadaki "View Full Forecast" bağlantısına tıkla! 📅`};
  }

  return {
    answer: `Şu an ${DEFAULT_LAT === 41.0082 ? 'İstanbul' : 'konumunuzda'} ${desc.toLowerCase()}, ${temp}°C. Nem %${humidity}, rüzgar ${wind} km/h. Daha fazlası için giriş yap! 🌤️`,
  };
}

// ── Ana Stiller ───────────────────────────────────────────────────────────────
const makeStyles = (C: typeof DarkColors) =>
  StyleSheet.create({
    safe: {flex: 1, backgroundColor: C.bg},

    // Header
    header: {
      flexDirection: 'row',
      alignItems: 'center',
      paddingHorizontal: Spacing.md,
      paddingVertical: Spacing.sm,
      borderBottomWidth: 0.5,
      borderBottomColor: C.divider,
    },
    backBtn: {width: 36},
    backArrow: {fontSize: 32, color: C.text, lineHeight: 36},
    headerCenter: {flex: 1, alignItems: 'center'},
    headerTitle: {fontSize: FontSize.lg, fontWeight: '700', color: C.text},
    onlineRow: {flexDirection: 'row', alignItems: 'center', gap: 4, marginTop: 1},
    onlineDot: {width: 7, height: 7, borderRadius: 4, backgroundColor: '#4CAF50'},
    onlineText: {fontSize: FontSize.xs, color: '#4CAF50', fontWeight: '600', letterSpacing: 0.5},
    infoBtn: {width: 36, alignItems: 'flex-end'},
    infoBtnText: {fontSize: 20, color: C.textSecondary},

    // Mesajlar
    messageList: {
      paddingVertical: Spacing.md,
      flexGrow: 1,
    },
    emptyState: {flex: 1, justifyContent: 'center', alignItems: 'center', paddingTop: 40},

    // Chip'ler
    chipsContainer: {
      paddingHorizontal: Spacing.md,
      paddingBottom: Spacing.sm,
      gap: Spacing.sm,
    },
    chip: {
      backgroundColor: C.bgCard,
      borderRadius: Radius.full,
      paddingHorizontal: Spacing.md,
      paddingVertical: Spacing.sm,
      borderWidth: 1,
      borderColor: C.border,
    },
    chipText: {
      fontSize: FontSize.sm,
      color: C.text,
      fontWeight: '500',
    },

    // Input
    inputBar: {
      flexDirection: 'row',
      alignItems: 'center',
      paddingHorizontal: Spacing.md,
      paddingVertical: Spacing.sm,
      borderTopWidth: 0.5,
      borderTopColor: C.divider,
      gap: Spacing.sm,
      backgroundColor: C.bg,
    },
    addBtn: {
      width: 36,
      height: 36,
      borderRadius: 18,
      borderWidth: 1.5,
      borderColor: C.border,
      justifyContent: 'center',
      alignItems: 'center',
    },
    addBtnText: {fontSize: 22, color: C.textSecondary, lineHeight: 26},
    input: {
      flex: 1,
      backgroundColor: C.bgCard,
      borderRadius: Radius.full,
      paddingHorizontal: Spacing.md,
      paddingVertical: 10,
      fontSize: FontSize.md,
      color: C.text,
      borderWidth: 1,
      borderColor: C.border,
    },
    sendBtn: {
      width: 40,
      height: 40,
      borderRadius: 20,
      backgroundColor: C.accentBtn,
      justifyContent: 'center',
      alignItems: 'center',
    },
    sendBtnDisabled: {opacity: 0.5},
    sendBtnIcon: {color: '#FFFFFF', fontSize: 14, marginLeft: 2},
  });
