import React, {useState, useEffect} from 'react';
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
  Image,
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';
import {useNavigation} from '@react-navigation/native';
import ImagePicker from 'react-native-image-crop-picker';

import {useAuthStore} from '../store/authStore';
import {useColors, AppColors} from '../theme';
import * as userService from '../services/userService';

export function ProfileEditScreen(): React.JSX.Element {
  const navigation = useNavigation();
  const {user, userProfile, updateProfile, refreshProfile, localProfileImage, setLocalProfileImage} = useAuthStore();
  const C = useColors();
  const s = makeStyles(C);

  const [formData, setFormData] = useState({
    name: '',
    bio: '',
    interests: '',
    assistantTone: 'DENGELI',
  });
  const [uploading, setUploading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (userProfile) {
      setFormData({
        name: userProfile.name || '',
        bio: userProfile.bio || '',
        interests: userProfile.interests?.join(', ') || '',
        assistantTone: userProfile.assistantTone || 'DENGELI',
      });
    }
  }, [userProfile]);

  const handlePickImage = async () => {
    try {
      const image = await ImagePicker.openPicker({
        width: 400,
        height: 400,
        cropping: true,
        includeBase64: false,
        mediaType: 'photo',
      });

      if (image.path) {
        setUploading(true);
        // Yerel saklama (AsyncStorage + Store)
        await setLocalProfileImage(image.path);

        // Bulut saklama (Firebase Auth varsa)
        if (user) {
          try {
            await userService.uploadProfileImage(user.uid, image.path);
            await refreshProfile();
          } catch (e) {
            console.warn('Cloud upload failed, but local saved.', e);
          }
        }
        Alert.alert('Başarılı', 'Profil fotoğrafın güncellendi.');
      }
    } catch (err: any) {
      if (err.code !== 'E_PICKER_CANCELLED') {
        Alert.alert('Hata', 'Fotoğraf yüklenemedi.');
      }
    } finally {
      setUploading(false);
    }
  };

  const handleRemoveImage = async () => {
    Alert.alert(
      'Fotoğrafı Kaldır',
      'Profil fotoğrafını silmek istediğine emin misin?',
      [
        { text: 'Vazgeç', style: 'cancel' },
        {
          text: 'Kaldır',
          style: 'destructive',
          onPress: async () => {
            setUploading(true);
            await setLocalProfileImage(null);
            if (user) {
              await userService.updateUserProfile(user.uid, { photoURL: '' });
              await refreshProfile();
            }
            setUploading(false);
          }
        }
      ]
    );
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      await updateProfile({
        name: formData.name,
        bio: formData.bio,
        interests: formData.interests.split(',').map(i => i.trim()).filter(i => i !== ''),
        assistantTone: formData.assistantTone,
      });
      Alert.alert('Başarılı', 'Profil bilgilerin güncellendi.');
      navigation.goBack();
    } catch (err) {
      Alert.alert('Hata', 'Bilgiler kaydedilemedi.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <SafeAreaView style={s.safe}>
      <ScrollView contentContainerStyle={s.content}>
        <View style={s.header}>
          <TouchableOpacity onPress={() => navigation.goBack()} style={s.backBtn}>
            <Icon name="arrow-back" size={24} color={C.text} />
          </TouchableOpacity>
          <Text style={s.headerTitle}>Profili Düzenle</Text>
          <View style={{width: 40}} />
        </View>

        <View style={s.avatarContainer}>
          <TouchableOpacity style={s.avatarWrapper} onPress={handlePickImage} disabled={uploading}>
            {((userProfile?.photoURL && userProfile.photoURL.length > 0) || (localProfileImage && localProfileImage.length > 0)) ? (
              <Image source={{uri: userProfile?.photoURL || localProfileImage || ''}} style={s.avatar} />
            ) : (
              <View style={[s.avatar, s.avatarPlaceholder]}>
                <Icon name="person" size={50} color={C.textMuted} />
              </View>
            )}
            <View style={s.editBadge}>
              {uploading ? <ActivityIndicator size="small" color="#FFF" /> : <Icon name="camera" size={16} color="#FFF" />}
            </View>
          </TouchableOpacity>
          <Text style={s.avatarHint}>Fotoğrafı değiştirmek için dokun</Text>
          {((userProfile?.photoURL && userProfile.photoURL.length > 0) || (localProfileImage && localProfileImage.length > 0)) && (
            <TouchableOpacity onPress={handleRemoveImage} style={s.removePhotoBtn}>
              <Text style={s.removePhotoText}>Fotoğrafı Kaldır</Text>
            </TouchableOpacity>
          )}
        </View>

        <View style={s.inputGroup}>
          <Text style={s.label}>Ad Soyad</Text>
          <TextInput
            style={s.textInput}
            value={formData.name}
            onChangeText={t => setFormData(p => ({...p, name: t}))}
            placeholder="Adın ve soyadın"
            placeholderTextColor={C.textMuted}
          />
        </View>

        <View style={s.inputGroup}>
          <Text style={s.label}>Hakkında (Bio)</Text>
          <TextInput
            style={[s.textInput, s.textArea]}
            value={formData.bio}
            onChangeText={t => setFormData(p => ({...p, bio: t}))}
            placeholder="Kendinden bahset..."
            placeholderTextColor={C.textMuted}
            multiline
          />
        </View>

        <View style={s.inputGroup}>
          <Text style={s.label}>İlgi Alanları</Text>
          <TextInput
            style={[s.textInput, s.textArea]}
            value={formData.interests}
            onChangeText={t => setFormData(p => ({...p, interests: t}))}
            placeholder="Doğa, Kayak, Gastronomi... (Virgülle ayırın)"
            placeholderTextColor={C.textMuted}
            multiline
          />
        </View>

        <View style={s.inputGroup}>
          <Text style={s.label}>Asistan Tonu</Text>
          <View style={s.toneContainer}>
            {['SAMIMI', 'RESMI', 'DENGELI'].map(tone => (
              <TouchableOpacity
                key={tone}
                style={[s.toneBtn, formData.assistantTone === tone && s.toneBtnActive]}
                onPress={() => setFormData(p => ({...p, assistantTone: tone}))}
              >
                <Text style={[s.toneText, formData.assistantTone === tone && s.toneTextActive]}>
                  {tone === 'SAMIMI' ? 'Samimi' : tone === 'RESMI' ? 'Resmi' : 'Dengeli'}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        <TouchableOpacity
          style={[s.saveBtn, saving && {opacity: 0.7}]}
          onPress={handleSave}
          disabled={saving}
        >
          {saving ? <ActivityIndicator color="#FFF" /> : <Text style={s.saveBtnText}>Değişiklikleri Kaydet</Text>}
        </TouchableOpacity>
      </ScrollView>
    </SafeAreaView>
  );
}

const makeStyles = (C: AppColors) => StyleSheet.create({
  safe: {flex: 1, backgroundColor: C.bg},
  content: {padding: 20},
  header: {flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 32},
  headerTitle: {fontSize: 20, fontWeight: '800', color: C.text},
  backBtn: {width: 40, height: 40, justifyContent: 'center', alignItems: 'center'},
  avatarContainer: {alignItems: 'center', marginBottom: 32},
  avatarWrapper: {width: 100, height: 100, borderRadius: 50, position: 'relative'},
  avatar: {width: 100, height: 100, borderRadius: 50, borderWidth: 2, borderColor: C.accent},
  avatarPlaceholder: {backgroundColor: C.bgCard, justifyContent: 'center', alignItems: 'center', borderColor: C.border},
  editBadge: {position: 'absolute', bottom: 0, right: 0, width: 32, height: 32, borderRadius: 16, backgroundColor: C.accent, justifyContent: 'center', alignItems: 'center', borderWidth: 2, borderColor: C.bg},
  avatarHint: {fontSize: 12, color: C.textSecondary, marginTop: 8},
  removePhotoBtn: {marginTop: 12, paddingVertical: 4, paddingHorizontal: 12},
  removePhotoText: {fontSize: 13, color: '#EF4444', fontWeight: '600'},
  inputGroup: {marginBottom: 20},
  label: {fontSize: 14, fontWeight: '700', color: C.text, marginBottom: 8, marginLeft: 4},
  textInput: {backgroundColor: C.bgCard, borderRadius: 14, padding: 14, color: C.text, borderWidth: 1, borderColor: C.border, fontSize: 16},
  textArea: {minHeight: 80, textAlignVertical: 'top'},
  toneContainer: {flexDirection: 'row', gap: 10},
  toneBtn: {flex: 1, paddingVertical: 12, borderRadius: 12, backgroundColor: C.bgCard, borderWidth: 1, borderColor: C.border, alignItems: 'center'},
  toneBtnActive: {backgroundColor: C.accent, borderColor: C.accent},
  toneText: {fontSize: 13, color: C.text, fontWeight: '700'},
  toneTextActive: {color: '#FFF'},
  saveBtn: {backgroundColor: C.accent, paddingVertical: 18, borderRadius: 16, alignItems: 'center', marginTop: 24},
  saveBtnText: {color: '#FFF', fontSize: 16, fontWeight: '800'},
});
