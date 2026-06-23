import React from 'react';
import {
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  Alert,
  ActivityIndicator,
} from 'react-native';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import Icon from 'react-native-vector-icons/Ionicons';
import {useNavigation} from '@react-navigation/native';

import {getProfile, updateProfile} from '../services/profileApi';
import {useColors, AppColors} from '../theme';

export function ProfileEditScreen(): React.JSX.Element {
  const queryClient = useQueryClient();
  const navigation = useNavigation();
  const C = useColors();
  const s = makeStyles(C);

  const {data: profile, isLoading} = useQuery({
    queryKey: ['profile'],
    queryFn: getProfile,
  });

  const [formData, setFormData] = React.useState({
    interest: '',
    health_sensitivities: '',
    travel_preferences: '',
    activity_types: '',
    assistant_tone: 'friendly',
  });

  React.useEffect(() => {
    if (profile) {
      setFormData({
        interest: profile.interest || '',
        health_sensitivities: profile.health_sensitivities || '',
        travel_preferences: profile.travel_preferences || '',
        activity_types: profile.activity_types || '',
        assistant_tone: profile.assistant_tone || 'friendly',
      });
    }
  }, [profile]);

  const updateMutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: () => {
      queryClient.invalidateQueries({queryKey: ['profile']});
      Alert.alert('Başarılı', 'Profil bilgilerin güncellendi.');
      navigation.goBack();
    },
    onError: () => Alert.alert('Hata', 'Güncelleme yapılamadı.'),
  });

  if (isLoading) {
    return (
      <View style={{flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: C.bg}}>
        <ActivityIndicator size="large" color={C.accent} />
      </View>
    );
  }

  return (
    <SafeAreaView style={s.safe}>
      <ScrollView contentContainerStyle={s.content}>
        <Text style={s.headerTitle}>AI Kişiselleştirme</Text>
        <Text style={s.headerDesc}>
          Asistanın sana daha iyi yardımcı olabilmesi için ilgi alanlarını ve hassasiyetlerini bizimle paylaşabilirsin.
        </Text>

        <View style={s.inputGroup}>
          <Text style={s.label}>Genel İlgi Alanları</Text>
          <TextInput
            style={s.textInput}
            placeholder="Örn: Doğa yürüyüşü, teknoloji, gastronomi..."
            value={formData.interest}
            onChangeText={t => setFormData(p => ({...p, interest: t}))}
            multiline
          />
        </View>

        <View style={s.inputGroup}>
          <Text style={s.label}>Sağlık Hassasiyetleri</Text>
          <TextInput
            style={s.textInput}
            placeholder="Örn: UV hassasiyeti, polen alerjisi, astım..."
            value={formData.health_sensitivities}
            onChangeText={t => setFormData(p => ({...p, health_sensitivities: t}))}
            multiline
          />
        </View>

        <View style={s.inputGroup}>
          <Text style={s.label}>Seyahat Tercihleri</Text>
          <TextInput
            style={s.textInput}
            placeholder="Örn: Sakin yerler, lüks konaklama, macera..."
            value={formData.travel_preferences}
            onChangeText={t => setFormData(p => ({...p, travel_preferences: t}))}
            multiline
          />
        </View>

        <View style={s.inputGroup}>
          <Text style={s.label}>Asistan Konuşma Tarzı</Text>
          <View style={s.toneContainer}>
            {['friendly', 'professional', 'short'].map(tone => (
              <TouchableOpacity
                key={tone}
                style={[s.toneBtn, formData.assistant_tone === tone && s.toneBtnActive]}
                onPress={() => setFormData(p => ({...p, assistant_tone: tone}))}
              >
                <Text style={[s.toneText, formData.assistant_tone === tone && s.toneTextActive]}>
                  {tone === 'friendly' ? 'Samimi' : tone === 'professional' ? 'Profesyonel' : 'Kısa & Öz'}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <TouchableOpacity
          style={s.saveBtn}
          onPress={() => updateMutation.mutate(formData)}
          disabled={updateMutation.isPending}
        >
          {updateMutation.isPending ? (
            <ActivityIndicator color="#FFF" />
          ) : (
            <Text style={s.saveBtnText}>Değişiklikleri Kaydet</Text>
          )}
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

const makeStyles = (C: AppColors) => StyleSheet.create({
  safe: {flex: 1, backgroundColor: C.bg},
  content: {padding: 20},
  headerTitle: {fontSize: 24, fontWeight: '800', color: C.text, marginBottom: 8},
  headerDesc: {fontSize: 14, color: C.textSecondary, marginBottom: 24, lineHeight: 20},
  inputGroup: {marginBottom: 20},
  label: {fontSize: 16, fontWeight: '700', color: C.text, marginBottom: 8},
  textInput: {
    backgroundColor: C.bgCard,
    borderRadius: 12,
    padding: 12,
    color: C.text,
    borderWidth: 1,
    borderColor: C.border,
    minHeight: 80,
    textAlignVertical: 'top',
  },
  toneContainer: {flexDirection: 'row', gap: 10},
  toneBtn: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 10,
    backgroundColor: C.bgCard,
    borderWidth: 1,
    borderColor: C.border,
    alignItems: 'center',
  },
  toneBtnActive: {backgroundColor: C.accent, borderColor: C.accent},
  toneText: {fontSize: 14, color: C.text, fontWeight: '600'},
  toneTextActive: {color: '#FFF'},
  saveBtn: {
    backgroundColor: C.accent,
    paddingVertical: 16,
    borderRadius: 14,
    alignItems: 'center',
    marginTop: 20,
    shadowColor: C.accent,
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 4,
  },
  saveBtnText: {color: '#FFF', fontSize: 16, fontWeight: '700'},
});
