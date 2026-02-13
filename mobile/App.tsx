import React from 'react';
import {SafeAreaProvider} from 'react-native-safe-area-context';
import {NavigationContainer, DefaultTheme, DarkTheme} from '@react-navigation/native';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import './src/i18n';

import {ErrorBoundary} from './src/components/ErrorBoundary';
import {ToastProvider} from './src/components/ToastProvider';
import {RootNavigator} from './src/navigation/RootNavigator';
import {initSentry} from './src/services/sentry';
import {useThemeStore} from './src/store/themeStore';

const queryClient = new QueryClient();
initSentry();

export default function App(): React.JSX.Element {
  console.log('[DEBUG] App: Rendering started');
  const theme = useThemeStore(state => state.theme);
  const navTheme = theme === 'dark' ? DarkTheme : DefaultTheme;

  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <SafeAreaProvider>
          <ToastProvider>
            <NavigationContainer theme={navTheme}>
              <RootNavigator />
            </NavigationContainer>
          </ToastProvider>
        </SafeAreaProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}
