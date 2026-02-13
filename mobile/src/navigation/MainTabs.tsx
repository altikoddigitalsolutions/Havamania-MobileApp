import React from 'react';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';

import {AlertsScreen} from '../screens/AlertsScreen';
import {ChatbotScreen} from '../screens/ChatbotScreen';
import {HomeScreen} from '../screens/HomeScreen';
import {MapScreen} from '../screens/MapScreen';
import {SettingsStack} from './SettingsStack';
import {MainTabParamList} from './types';

const Tab = createBottomTabNavigator<MainTabParamList>();

export function MainTabs(): React.JSX.Element {
  return (
    <Tab.Navigator>
      <Tab.Screen name="Home" component={HomeScreen} />
      <Tab.Screen name="Map" component={MapScreen} />
      <Tab.Screen name="Alerts" component={AlertsScreen} />
      <Tab.Screen name="Chatbot" component={ChatbotScreen} />
      <Tab.Screen name="Settings" component={SettingsStack} options={{headerShown: false}} />
    </Tab.Navigator>
  );
}
