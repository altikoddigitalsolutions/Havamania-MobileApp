import React from 'react';
import {createNativeStackNavigator} from '@react-navigation/native-stack';

import {LocationManagementScreen} from '../screens/LocationManagementScreen';
import {PaywallScreen} from '../screens/PaywallScreen';
import {SettingsScreen} from '../screens/SettingsScreen';

export type SettingsStackParamList = {
  SettingsHome: undefined;
  LocationManagement: undefined;
  Paywall: undefined;
};

const Stack = createNativeStackNavigator<SettingsStackParamList>();

export function SettingsStack(): React.JSX.Element {
  return (
    <Stack.Navigator>
      <Stack.Screen name="SettingsHome" component={SettingsScreen} options={{title: 'Settings'}} />
      <Stack.Screen
        name="LocationManagement"
        component={LocationManagementScreen}
        options={{title: 'Locations'}}
      />
      <Stack.Screen name="Paywall" component={PaywallScreen} options={{title: 'Premium'}} />
    </Stack.Navigator>
  );
}
