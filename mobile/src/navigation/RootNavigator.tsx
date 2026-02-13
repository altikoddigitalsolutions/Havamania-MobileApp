import React from 'react';
import {ActivityIndicator, View, Text} from 'react-native';

import {AuthNavigator} from './AuthNavigator';
import {MainTabs} from './MainTabs';
import {useAuthStore} from '../store/authStore';

export function RootNavigator(): React.JSX.Element {
  const {isAuthenticated, initializing, initSession} = useAuthStore();

  React.useEffect(() => {
    console.log('[DEBUG] RootNavigator: initSession starting...');
    initSession()
      .then(() => console.log('[DEBUG] RootNavigator: initSession finished'))
      .catch((err) => console.error('[DEBUG] RootNavigator: initSession error', err));
  }, [initSession]);

  console.log('[DEBUG] RootNavigator State:', {isAuthenticated, initializing});

  if (initializing) {
    return (
      <View style={{flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: '#ffffff'}}>
        <ActivityIndicator size="large" color="#0000ff" />
        <Text style={{marginTop: 10, color: '#333333'}}>Havamania yukleniyor...</Text>
      </View>
    );
  }

  return isAuthenticated ? <MainTabs /> : <AuthNavigator />;
}
