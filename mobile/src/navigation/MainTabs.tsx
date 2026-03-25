import React from 'react';
import {GestureResponderEvent, StyleSheet, Text, TouchableOpacity, View} from 'react-native';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';

import {HomeScreen} from '../screens/HomeScreen';
import {ChatbotScreen} from '../screens/ChatbotScreen';
import {ProfileScreen} from '../screens/ProfileScreen';
import {MainTabParamList} from './types';
import {AppColors, DarkColors, FontSize, LightColors} from '../theme';
import {useThemeStore} from '../store/themeStore';

const Tab = createBottomTabNavigator<MainTabParamList>();

export function MainTabs(): React.JSX.Element {
  const {theme} = useThemeStore();
  const C = theme === 'dark' ? DarkColors : LightColors;

  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarStyle: {
          backgroundColor: C.tabBar,
          borderTopColor: C.border,
          borderTopWidth: 0.5,
          height: 60,
          paddingBottom: 8,
        },
        tabBarActiveTintColor: C.tabActive,
        tabBarInactiveTintColor: C.tabInactive,
        tabBarLabelStyle: {
          fontSize: FontSize.xs,
          fontWeight: '700',
          letterSpacing: 0.5,
        },
      }}>

      {/* Hava Durumu */}
      <Tab.Screen
        name="Weather"
        component={HomeScreen}
        options={{
          tabBarLabel: 'WEATHER',
          tabBarIcon: ({color}) => (
            <Text style={{fontSize: 20, color}}>🌤️</Text>
          ),
        }}
      />

      {/* AI Chat — Orta, yüzen buton */}
      <Tab.Screen
        name="AIChat"
        component={ChatbotScreen}
        options={{
          tabBarLabel: '',
          // eslint-disable-next-line react/no-unstable-nested-components
          tabBarButton: props => (
            <FloatingAIButton onPress={() => (props.onPress as any)?.()} C={C} />
          ),
        }}
      />

      {/* Profil */}
      <Tab.Screen
        name="Profile"
        component={ProfileScreen}
        options={{
          tabBarLabel: 'PROFILE',
          tabBarIcon: ({color}) => (
            <Text style={{fontSize: 20, color}}>👤</Text>
          ),
        }}
      />
    </Tab.Navigator>
  );
}

// ── Yüzen AI Chat Butonu ─────────────────────────────────────────────────────
function FloatingAIButton({
  onPress,
  C,
}: {
  onPress?: () => void;
  C: AppColors;
}) {
  return (
    <View style={floatStyles.wrapper}>
      <TouchableOpacity
        style={[floatStyles.btn, {backgroundColor: C.accentBtn}]}
        onPress={onPress}
        activeOpacity={0.85}>
        <Text style={floatStyles.btnIcon}>🤖</Text>
      </TouchableOpacity>
      <Text style={[floatStyles.label, {color: C.tabInactive}]}>AI CHAT</Text>
    </View>
  );
}

const floatStyles = StyleSheet.create({
  wrapper: {
    alignItems: 'center',
    justifyContent: 'flex-end',
    paddingBottom: 4,
    width: 80,
  },
  btn: {
    width: 56,
    height: 56,
    borderRadius: 28,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 2,
    shadowColor: '#1A8EF0',
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.4,
    shadowRadius: 8,
    elevation: 8,
    // Tab bar dışına taşması için negatif margin
    marginTop: -20,
  },
  btnIcon: {fontSize: 26},
  label: {fontSize: 9, fontWeight: '700', letterSpacing: 0.5},
});
