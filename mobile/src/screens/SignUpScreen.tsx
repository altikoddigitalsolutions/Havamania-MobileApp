import React, {useMemo, useState} from 'react';
import {Alert, Button, StyleSheet, Text, TextInput, View} from 'react-native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';

import {AuthStackParamList} from '../navigation/types';
import {useAuthStore} from '../store/authStore';

type Props = NativeStackScreenProps<AuthStackParamList, 'SignUp'>;

export function SignUpScreen({navigation}: Props): React.JSX.Element {
  const signupWithEmail = useAuthStore(state => state.signupWithEmail);
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const isValid = useMemo(() => {
    return email.includes('@') && password.length >= 8 && password === confirmPassword;
  }, [email, password, confirmPassword]);

  const handleSignup = async () => {
    if (!isValid) {
      Alert.alert('Hata', 'Bilgileri kontrol edin.');
      return;
    }

    try {
      setLoading(true);
      await signupWithEmail(email.trim(), password, fullName.trim());
    } catch {
      Alert.alert('Hata', 'Kayit basarisiz.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Hesap Olustur</Text>
      <TextInput style={styles.input} placeholder="Ad Soyad" value={fullName} onChangeText={setFullName} />
      <TextInput style={styles.input} placeholder="E-posta" value={email} onChangeText={setEmail} />
      <TextInput
        style={styles.input}
        placeholder="Sifre"
        secureTextEntry
        value={password}
        onChangeText={setPassword}
      />
      <TextInput
        style={styles.input}
        placeholder="Sifre Tekrar"
        secureTextEntry
        value={confirmPassword}
        onChangeText={setConfirmPassword}
      />
      <Button title={loading ? 'Bekleyin...' : 'Kaydol'} onPress={handleSignup} disabled={loading} />
      <View style={styles.spacer} />
      <Button title="Giris ekranina don" onPress={() => navigation.navigate('Login')} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, justifyContent: 'center', padding: 16, gap: 12},
  title: {fontSize: 24, fontWeight: '700', marginBottom: 12, textAlign: 'center'},
  input: {borderWidth: 1, borderColor: '#d1d5db', borderRadius: 8, padding: 10},
  spacer: {height: 8},
});
