import React, {useMemo, useState} from 'react';
import {Alert, Button, StyleSheet, Text, TextInput, View} from 'react-native';
import {NativeStackScreenProps} from '@react-navigation/native-stack';

import {AuthStackParamList} from '../navigation/types';
import {useAuthStore} from '../store/authStore';

type Props = NativeStackScreenProps<AuthStackParamList, 'Login'>;

export function LoginScreen({navigation}: Props): React.JSX.Element {
  const loginWithEmail = useAuthStore(state => state.loginWithEmail);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const isValid = useMemo(() => email.includes('@') && password.length >= 8, [email, password]);

  const handleLogin = async () => {
    if (!isValid) {
      Alert.alert('Hata', 'Gecerli e-posta ve en az 8 karakter sifre girin.');
      return;
    }

    try {
      setLoading(true);
      await loginWithEmail(email.trim(), password);
    } catch {
      Alert.alert('Hata', 'Giris basarisiz.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Havamania</Text>
      <TextInput style={styles.input} placeholder="E-posta" value={email} onChangeText={setEmail} />
      <TextInput
        style={styles.input}
        placeholder="Sifre"
        secureTextEntry
        value={password}
        onChangeText={setPassword}
      />
      <Button title={loading ? 'Bekleyin...' : 'Giris Yap'} onPress={handleLogin} disabled={loading} />
      <View style={styles.spacer} />
      <Button title="Kaydol" onPress={() => navigation.navigate('SignUp')} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {flex: 1, justifyContent: 'center', padding: 16, gap: 12},
  title: {fontSize: 24, fontWeight: '700', marginBottom: 12, textAlign: 'center'},
  input: {borderWidth: 1, borderColor: '#d1d5db', borderRadius: 8, padding: 10},
  spacer: {height: 8},
});
