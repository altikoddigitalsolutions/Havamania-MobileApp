import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';

import {MainStackParamList} from './types';
import {MainTabs} from './MainTabs';
import {ForecastScreen} from '../screens/ForecastScreen';
import {HourlyScreen} from '../screens/HourlyScreen';
import {WeatherDetailScreen} from '../screens/WeatherDetailScreen';
import {AlertsScreen} from '../screens/AlertsScreen';
import {MapScreen} from '../screens/MapScreen';

const Stack = createNativeStackNavigator<MainStackParamList>();

export function MainStack(): React.JSX.Element {
  return (
    <Stack.Navigator screenOptions={{headerShown: false}}>
      {/* Tab bar'ı içeren kapsayıcı ekran */}
      <Stack.Screen name="Tabs" component={MainTabs} />

      {/* Tab bar olmadan tam ekran açılan sayfalar */}
      <Stack.Screen
        name="Forecast"
        component={ForecastScreen}
        options={{animation: 'slide_from_right'}}
      />
      <Stack.Screen
        name="Hourly"
        component={HourlyScreen}
        options={{animation: 'slide_from_right'}}
      />
      <Stack.Screen
        name="WeatherDetail"
        component={WeatherDetailScreen}
        options={{animation: 'slide_from_right'}}
      />
      <Stack.Screen
        name="Alerts"
        component={AlertsScreen}
        options={{animation: 'slide_from_bottom'}}
      />
      <Stack.Screen
        name="Map"
        component={MapScreen}
        options={{animation: 'slide_from_bottom'}}
      />
    </Stack.Navigator>
  );
}
