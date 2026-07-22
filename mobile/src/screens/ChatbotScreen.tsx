import React, {useEffect, useRef, useState, useMemo} from 'react';
import {
  ActivityIndicator,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  Dimensions,
  ScrollView,
  Animated,
} from 'react-native';
import {useMutation, useQuery} from '@tanstack/react-query';
import {useNavigation} from '@react-navigation/native';
import Icon from 'react-native-vector-icons/Ionicons';

import {askChatbot} from '../services/chatbotApi';
import {getCurrentWeather} from '../services/weatherApi';
import {Spacing, Radius, FontSize, useColors, getWeatherEmoji, getWeatherLabel} from '../theme';
import {useThemeStore} from '../store/themeStore';
import {useAuthStore} from '../store/authStore';

// Safe import for LinearGradient
let LinearGradient: any;
try {
  LinearGradient = require('react-native-linear-gradient').default;
} catch (e) {
  LinearGradient = ({ children, colors, style }: any) => (
    <View style={[style, { backgroundColor: colors[0] }]}>{children}</View>
  );
}

const { width } = Dimensions.get('window');

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
const DEFAULT_CITY = 'İstanbul';

// ── Hızlı Aksiyonlar ───────────────────────────────────────────────────────────
const QUICK_ACTIONS = [
  { id: 'wear', label: 'Ne giymeliyim?', icon: 'shirt-outline', color: '#60A5FA' },
  { id: 'travel', label: 'Seyahat planla', icon: 'map-outline', color: '#10B981' },
  { id: 'picnic', label: 'Piknik uygun mu?', icon: 'basket-outline', color: '#F59E0B' },
  { id: 'photo', label: 'Fotoğraf saati', icon: 'camera-outline', color: '#EC4899' },
];

const SUGGESTIONS = [
  'Bugün yağmur yağacak mı?',
  'Hafta sonu planı için hava nasıl?',
  'Dışarıda spor yapmak için en iyi saat?',
  'UV İndeksi raporu verir misin?',
];

export function ChatbotScreen(): React.JSX.Element {
  const navigation = useNavigation();
  const C = useColors();
  const {theme} = useThemeStore();
  const {isGuest, user, userProfile} = useAuthStore();
  const flatRef = useRef<FlatList>(null);

  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  const weatherQuery = useQuery({
    queryKey: ['weather', 'current', DEFAULT_LAT, DEFAULT_LON],
    queryFn: () => getCurrentWeather(DEFAULT_LAT, DEFAULT_LON),
    staleTime: 5 * 60 * 1000,
  });

  const askMutation = useMutation({
    mutationFn: (question: string) => {
      const tone = userProfile?.assistantTone || 'DENGELI';
      if (isGuest) {
        return new Promise((resolve) => {
            setTimeout(() => resolve(buildLocalAnswer(question, weatherQuery.data, userProfile, tone)), 1000);
        });
      }
      return askChatbot(question, tone);
    },
    onMutate: () => setIsTyping(true),
    onSuccess: (data: any, question: string) => {
      setIsTyping(false);
      const answer = data?.answer ?? data?.content ?? String(data);
      const isWeatherQ = /hava|tahmin|sıcaklık|yağmur|güneş|rüzgar|uv/i.test(question);

      setMessages(prev => {
        const next = [...prev];
        next.push({
          id: `${Date.now()}-assistant`,
          role: 'assistant',
          content: answer,
        });

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
      setIsTyping(false);
      setMessages(prev => [
        ...prev,
        {
          id: `${Date.now()}-err`,
          role: 'assistant',
          content: 'Şu an bir sorun oluştu. Lütfen tekrar deneyin.',
        },
      ]);
    },
  });

  const send = (text?: string) => {
    const question = (text ?? input).trim();
    if (!question || isTyping) return;

    setMessages(prev => [
      ...prev,
      {id: `${Date.now()}-user`, role: 'user', content: question},
    ]);
    setInput('');
    askMutation.mutate(question);
  };

  useEffect(() => {
    if (messages.length > 0) {
      setTimeout(() => flatRef.current?.scrollToEnd({animated: true}), 100);
    }
  }, [messages, isTyping]);

  return (
    <SafeAreaView style={[styles.container, {backgroundColor: C.bg}]}>
      <StatusBar barStyle={theme === 'light' ? 'dark-content' : 'light-content'} />

      {/* Header */}
      <View style={[styles.header, {borderBottomColor: C.divider}]}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backButton}>
          <Icon name="chevron-back" size={28} color={C.text} />
        </TouchableOpacity>
        <View style={styles.headerTitleContainer}>
          <Text style={[styles.headerTitle, {color: C.text}]}>Havamania AI</Text>
          <View style={styles.statusRow}>
            <View style={[styles.statusDot, {backgroundColor: '#10B981'}]} />
            <Text style={[styles.statusText, {color: '#10B981'}]}>AKTİF</Text>
          </View>
        </View>
        <TouchableOpacity style={styles.historyButton}>
          <Icon name="time-outline" size={24} color={C.textSecondary} />
        </TouchableOpacity>
      </View>

      <KeyboardAvoidingView
        style={{flex: 1}}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}>

        {messages.length === 0 ? (
          <WelcomeView
            userName={userProfile?.name || (isGuest ? 'Misafir' : 'Gezgin')}
            onAction={send}
            onSuggestion={send}
            C={C}
          />
        ) : (
          <FlatList
            ref={flatRef}
            data={messages}
            keyExtractor={item => item.id}
            contentContainerStyle={styles.messageList}
            showsVerticalScrollIndicator={false}
            renderItem={({item}) => {
              if (item.role === 'weather-card' && item.weatherData) {
                return <WeatherCard data={item.weatherData} C={C} city={DEFAULT_CITY} />;
              }
              return <MessageBubble message={item} C={C} />;
            }}
            ListFooterComponent={isTyping ? <TypingIndicator C={C} /> : null}
          />
        )}

        {/* Input Area */}
        <View style={[styles.inputContainer, {backgroundColor: C.bgSecondary, borderTopColor: C.divider}]}>
          <View style={[styles.inputWrapper, {backgroundColor: theme === 'dark' ? '#1F2937' : '#F3F4F6'}]}>
            <TouchableOpacity style={styles.attachBtn}>
              <Icon name="add-circle-outline" size={24} color={C.textSecondary} />
            </TouchableOpacity>
            <TextInput
              style={[styles.input, {color: C.text}]}
              placeholder="Hava durumunu sor..."
              placeholderTextColor={C.textMuted}
              value={input}
              onChangeText={setInput}
              multiline
              maxHeight={100}
            />
            <TouchableOpacity
              onPress={() => send()}
              disabled={!input.trim() || isTyping}
              style={[styles.sendButton, {backgroundColor: input.trim() ? C.accent : C.textMuted + '40'}]}
            >
              <Icon name="arrow-up" size={20} color="#FFF" />
            </TouchableOpacity>
          </View>
        </View>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function WelcomeView({ userName, onAction, onSuggestion, C }: { userName: string, onAction: (s: string) => void, onSuggestion: (s: string) => void, C: any }) {
  return (
    <ScrollView contentContainerStyle={styles.welcomeContainer} showsVerticalScrollIndicator={false}>
      <View style={styles.welcomeHeader}>
        <View style={[styles.aiIcon, {backgroundColor: C.accent + '20'}]}>
          <Icon name="sparkles" size={40} color={C.accent} />
        </View>
        <Text style={[styles.welcomeTitle, {color: C.text}]}>Merhaba, {userName}</Text>
        <Text style={[styles.welcomeSubtitle, {color: C.textSecondary}]}>Hava durumuna göre gününü planlamana nasıl yardımcı olabilirim?</Text>
      </View>

      <Text style={[styles.sectionLabel, {color: C.textSecondary}]}>HIZLI AKSİYONLAR</Text>
      <View style={styles.actionsGrid}>
        {QUICK_ACTIONS.map(action => (
          <TouchableOpacity
            key={action.id}
            style={[styles.actionCard, {backgroundColor: C.bgSecondary, borderColor: C.divider}]}
            onPress={() => onAction(action.label)}
          >
            <View style={[styles.actionIconBox, {backgroundColor: action.color + '15'}]}>
              <Icon name={action.icon} size={22} color={action.color} />
            </View>
            <Text style={[styles.actionLabel, {color: C.text}]}>{action.label}</Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={[styles.sectionLabel, {color: C.textSecondary, marginTop: Spacing.lg}]}>ÖNERİLEN SORULAR</Text>
      <View style={styles.suggestionsList}>
        {SUGGESTIONS.map((suggestion, idx) => (
          <TouchableOpacity
            key={idx}
            style={[styles.suggestionItem, {borderColor: C.divider}]}
            onPress={() => onSuggestion(suggestion)}
          >
            <Text style={[styles.suggestionText, {color: C.text}]}>{suggestion}</Text>
            <Icon name="chevron-forward" size={16} color={C.textMuted} />
          </TouchableOpacity>
        ))}
      </View>
    </ScrollView>
  );
}

function MessageBubble({ message, C }: { message: Message, C: any }) {
  const isUser = message.role === 'user';
  return (
    <View style={[styles.bubbleWrapper, isUser ? styles.userBubbleWrapper : styles.botBubbleWrapper]}>
      {!isUser && (
        <View style={[styles.botAvatar, {backgroundColor: C.accent}]}>
          <Icon name="sparkles" size={14} color="#FFF" />
        </View>
      )}
      <View style={[
        styles.bubble,
        isUser ? [styles.userBubble, {backgroundColor: C.accent}] : [styles.botBubble, {backgroundColor: C.bgSecondary, borderColor: C.divider}]
      ]}>
        <Text style={[styles.bubbleText, {color: isUser ? '#FFF' : C.text}]}>{message.content}</Text>
      </View>
    </View>
  );
}

function TypingIndicator({ C }: { C: any }) {
  return (
    <View style={styles.botBubbleWrapper}>
      <View style={[styles.botAvatar, {backgroundColor: C.accent}]}>
        <Icon name="sparkles" size={14} color="#FFF" />
      </View>
      <View style={[styles.bubble, styles.botBubble, {backgroundColor: C.bgSecondary, borderColor: C.divider, paddingVertical: 12}]}>
        <ActivityIndicator size="small" color={C.accent} />
      </View>
    </View>
  );
}

function WeatherCard({ data, C, city }: { data: any, C: any, city: string }) {
  return (
    <View style={styles.cardWrapper}>
      <LinearGradient colors={[C.accent, C.accentDark]} style={styles.weatherCard} start={{x:0, y:0}} end={{x:1, y:1}}>
        <View style={styles.cardHeader}>
          <Text style={styles.cardLabel}>HAVA DURUMU RAPORU</Text>
          <Text style={styles.cardCity}>{city}</Text>
        </View>
        <View style={styles.cardMain}>
          <Text style={styles.cardTemp}>{data.temperature}°</Text>
          <View style={styles.cardInfo}>
            <Text style={styles.cardDesc}>{getWeatherLabel(data.weather_code)}</Text>
            <Text style={styles.cardFeels}>Hissedilen: {data.feels_like}°</Text>
          </View>
          <Text style={styles.cardEmoji}>{getWeatherEmoji(data.weather_code)}</Text>
        </View>
        <View style={styles.cardStats}>
          <View style={styles.cardStat}><Text style={styles.statVal}>%{data.humidity}</Text><Text style={styles.statLabel}>NEM</Text></View>
          <View style={styles.cardStat}><Text style={styles.statVal}>{data.wind_speed}</Text><Text style={styles.statLabel}>RÜZGAR</Text></View>
          <View style={styles.cardStat}><Text style={styles.statVal}>{data.uv_index}</Text><Text style={styles.statLabel}>UV</Text></View>
        </View>
      </LinearGradient>
    </View>
  );
}

function buildLocalAnswer(question: string, weather: any, profile: any, tone: string = 'DENGELI'): {answer: string} {
  if (!weather) return {answer: 'Hava durumu bilgisi yükleniyor, lütfen bekle.'};
  const q = question.toLowerCase();
  const temp = weather.temperature;
  const desc = getWeatherLabel(weather.weather_code);
  const precip = weather.precipitation_probability || 0;
  const wind = weather.wind_speed || 0;
  const uv = weather.uv_index || 0;

  const interests = profile?.interests?.join(', ').toLowerCase() || "";
  const health = profile?.bio?.toLowerCase() || ""; // Bio'yu hassasiyetler için geçici kullanıyoruz

  if (tone === 'KISA_NET') {
    return { answer: `Hava ${desc}, ${temp}°C. Yağış ihtimali %${precip}. Rüzgar ${wind} km/s.` };
  }

  // Giyim Önerisi
  if (/wear|giy|kıyafet|kombin/i.test(q)) {
    let advice = "";
    if (temp > 28) advice = "İnce pamuklu tişört ve şort harika olur. Güneş gözlüğü ve şapka eklemeyi unutma. 😎";
    else if (temp > 18) advice = "Hafif bir sweatshirt veya ince bir ceket yeterli olacaktır. 👕";
    else advice = "Hava serin, kalın bir mont ve atkı almanı öneririm. 🧥";

    if (precip > 30) advice += "\n\n☔ Ayrıca yağış ihtimaline karşı şemsiyeni yanına almalısın.";

    return { answer: `Sıcaklık **${temp}°C**. ${advice}` };
  }

  // Valiz Önerisi
  if (/suitcase|valiz|çanta|hazırla/i.test(q)) {
    const items = ["✓ Powerbank", "✓ Şarj aleti"];
    if (temp > 25) items.push("✓ Tişörtler", "✓ Şortlar", "✓ Güneş kremi");
    else items.push("✓ Kalın kıyafetler", "✓ Hırka");

    if (precip > 30) items.push("✓ Şemsiye");
    if (wind > 35) items.push("✓ Rüzgarlık");

    return {
      answer: `Hava durumuna göre valizine şunları koymanı öneririm:\n\n${items.join('\n')}\n\nKeyifli yolculuklar! ✈️`
    };
  }

  // Takvim Optimizasyonu
  if (/calendar|takvim|optimize/i.test(q)) {
    return {
      answer: "Takviminizde yaklaşan bir etkinlik bulunamadı. Yeni bir plan eklediğinizde hava durumuna göre en uygun günleri belirleyebilirim. 📅"
    };
  }

  // Aktivite
  if (/picnic|piknik|yürüyüş|koşu|spor/i.test(q)) {
    const isGood = precip < 20 && wind < 25 && temp > 15 && temp < 30;

    if (interests.includes("koşu") && temp < 25 && precip < 10) {
        return { answer: "Koşu tutkun için harika bir hava! Sıcaklık ideal, nem düşük. Koşu ayakkabılarını hazırla! 🏃‍♂️" };
    }

    if (isGood) {
      return { answer: `Bugün dışarıda vakit geçirmek için **mükemmel** bir gün! Hava ${desc.toLowerCase()}, sıcaklık ${temp}°C. Keyfini çıkar! 🧺` };
    }
    return { answer: `Hava koşulları (${desc.toLowerCase()}) dış mekan aktiviteleri için pek elverişli görünmüyor. 💨` };
  }

  // Sağlık
  if (health.includes("uv") && uv > 5) {
      return { answer: `Hassasiyetini dikkate aldığımızda, bugünkü ${uv} seviyesindeki UV indeksi senin için riskli olabilir. Lütfen yüksek korumalı güneş kremi kullan. 🧴` };
  }

  // Hafta Sonu
  if (/weekend|hafta sonu|cumartesi|pazar/i.test(q)) {
    return {
      answer: `Hafta sonu için ${temp}°C civarında ${desc.toLowerCase()} bir hava bekleniyor. Cumartesi günü açık hava etkinlikleri için daha elverişli olabilir. ☀️`
    };
  }

  // Genel Analiz
  if (tone === 'DETAYLI_UZMAN') {
    return {
      answer: `ANALİZ: ${DEFAULT_CITY} istasyonunda hava ${desc.toLowerCase()}. Termal denge: ${temp}°C. Nem: %${weather.humidity}. UV: ${uv}. Rüzgar hızı ${wind} km/s. Koşullar dış mekan aktiviteleri için ${precip < 30 ? 'stabil' : 'riskli'} seyrediyor.`
    };
  }

  return {
    answer: `Şu an ${DEFAULT_CITY} için hava **${desc.toLowerCase()}**, sıcaklık ise **${temp}°C**. Yağış ihtimali %${precip}. Başka nasıl yardımcı olabilirim? 🌤️`,
  };
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    borderBottomWidth: 0.5,
  },
  backButton: { width: 40 },
  headerTitleContainer: { flex: 1, alignItems: 'center' },
  headerTitle: { fontSize: FontSize.lg, fontWeight: '800' },
  statusRow: { flexDirection: 'row', alignItems: 'center', gap: 4, marginTop: 2 },
  statusDot: { width: 6, height: 6, borderRadius: 3 },
  statusText: { fontSize: 10, fontWeight: '900', letterSpacing: 1 },
  historyButton: { width: 40, alignItems: 'flex-end' },

  welcomeContainer: { padding: Spacing.lg, paddingBottom: 100 },
  welcomeHeader: { alignItems: 'center', marginVertical: Spacing.xl },
  aiIcon: { width: 80, height: 80, borderRadius: 40, justifyContent: 'center', alignItems: 'center', marginBottom: Spacing.md },
  welcomeTitle: { fontSize: 28, fontWeight: '800', marginBottom: Spacing.xs },
  welcomeSubtitle: { fontSize: 16, textAlign: 'center', lineHeight: 22, paddingHorizontal: Spacing.md },

  sectionLabel: { fontSize: 11, fontWeight: '900', letterSpacing: 1.5, marginBottom: Spacing.md },
  actionsGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  actionCard: {
    width: (width - Spacing.lg * 2 - 12) / 2,
    padding: Spacing.md,
    borderRadius: Radius.lg,
    borderWidth: 1,
    alignItems: 'flex-start'
  },
  actionIconBox: { width: 40, height: 40, borderRadius: 12, justifyContent: 'center', alignItems: 'center', marginBottom: Spacing.sm },
  actionLabel: { fontSize: 14, fontWeight: '700' },

  suggestionsList: { gap: 10 },
  suggestionItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: Spacing.md,
    borderRadius: Radius.md,
    borderWidth: 1
  },
  suggestionText: { fontSize: 14, fontWeight: '600' },

  messageList: { padding: Spacing.md, paddingBottom: 20 },
  bubbleWrapper: { marginBottom: Spacing.lg, maxWidth: '85%' },
  userBubbleWrapper: { alignSelf: 'flex-end' },
  botBubbleWrapper: { alignSelf: 'flex-start', flexDirection: 'row', gap: 8 },
  botAvatar: { width: 28, height: 28, borderRadius: 14, justifyContent: 'center', alignItems: 'center', marginTop: 4 },
  bubble: { padding: Spacing.md, borderRadius: 20 },
  userBubble: { borderBottomRightRadius: 4 },
  botBubble: { borderBottomLeftRadius: 4, borderWidth: 1 },
  bubbleText: { fontSize: 15, lineHeight: 22, fontWeight: '500' },

  inputContainer: { padding: Spacing.md, paddingBottom: Platform.OS === 'ios' ? Spacing.lg : Spacing.md, borderTopWidth: 0.5 },
  inputWrapper: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 30 },
  attachBtn: { padding: 4 },
  input: { flex: 1, paddingHorizontal: 12, paddingVertical: 8, fontSize: 16 },
  sendButton: { width: 36, height: 36, borderRadius: 18, justifyContent: 'center', alignItems: 'center' },

  cardWrapper: { paddingHorizontal: Spacing.sm, marginBottom: Spacing.lg, marginLeft: 36 },
  weatherCard: { borderRadius: 24, padding: 20, overflow: 'hidden' },
  cardLabel: { color: 'rgba(255,255,255,0.7)', fontSize: 10, fontWeight: '900', letterSpacing: 1 },
  cardCity: { color: '#FFF', fontSize: 16, fontWeight: '800', marginTop: 2 },
  cardMain: { flexDirection: 'row', alignItems: 'center', marginVertical: 16 },
  cardTemp: { color: '#FFF', fontSize: 48, fontWeight: '200' },
  cardInfo: { flex: 1, marginLeft: 12 },
  cardDesc: { color: '#FFF', fontSize: 18, fontWeight: '700' },
  cardFeels: { color: 'rgba(255,255,255,0.8)', fontSize: 12, fontWeight: '600' },
  cardEmoji: { fontSize: 40 },
  cardStats: { flexDirection: 'row', justifyContent: 'space-between', borderTopWidth: 1, borderTopColor: 'rgba(255,255,255,0.2)', paddingTop: 12 },
  cardStat: { alignItems: 'center' },
  statVal: { color: '#FFF', fontSize: 14, fontWeight: '800' },
  statLabel: { color: 'rgba(255,255,255,0.7)', fontSize: 8, fontWeight: '900' },
});
