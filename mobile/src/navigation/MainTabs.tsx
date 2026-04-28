import React from 'react';
import {StyleSheet, Text, TouchableOpacity, View, Platform} from 'react-native';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import Icon from 'react-native-vector-icons/Ionicons';
import {useSafeAreaInsets} from 'react-native-safe-area-context';

import {HomeScreen} from '../screens/HomeScreen';
import {ChatbotScreen} from '../screens/ChatbotScreen';
import {ProfileScreen} from '../screens/ProfileScreen';
import {MainTabParamList} from './types';
import {useThemeStore} from '../store/themeStore';
import {useColors, AppColors} from '../theme';

const Tab = createBottomTabNavigator<MainTabParamList>();

export function MainTabs(): React.JSX.Element {
  const {theme} = useThemeStore();
  const insets = useSafeAreaInsets();
  const C = useColors();

  const TAB_BAR_HEIGHT = Platform.OS === 'ios' ? 60 + insets.bottom : 70;

  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarStyle: {
          backgroundColor: C.tabBar,
          borderTopColor: C.border,
          borderTopWidth: StyleSheet.hairlineWidth,
          height: TAB_BAR_HEIGHT,
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          elevation: 0,
          paddingTop: 8,
          borderTopLeftRadius: 24,
          borderTopRightRadius: 24,
          shadowColor: '#000',
          shadowOffset: {width: 0, height: -8},
          shadowOpacity: 0.1,
          shadowRadius: 15,
        },
        tabBarActiveTintColor: C.tabActive,
        tabBarInactiveTintColor: C.tabInactive,
        tabBarLabelStyle: {
          fontSize: 11,
          fontWeight: '600',
          marginBottom: Platform.OS === 'ios' ? 0 : 8,
        },
      }}>

      <Tab.Screen
        name="Weather"
        component={HomeScreen}
        options={{
          tabBarLabel: 'Hava',
          tabBarIcon: ({focused}) => (
            <View style={styles.iconContainer}>
              <Text style={{fontSize: 22}}>{focused ? "🌤️" : "☁️"}</Text>
              {focused && <View style={[styles.indicator, {backgroundColor: C.tabActive}]} />}
            </View>
          ),
        }}
      />

      <Tab.Screen
        name="AIChat"
        component={ChatbotScreen}
        options={{
          tabBarLabel: '',
          tabBarButton: props => (
            <AIButton onPress={() => (props.onPress as any)?.()} C={C} />
          ),
        }}
      />

      <Tab.Screen
        name="Profile"
        component={ProfileScreen}
        options={{
          tabBarLabel: 'Profil',
          tabBarIcon: ({focused}) => (
            <View style={styles.iconContainer}>
              <Text style={{fontSize: 22}}>{focused ? "👤" : "👤"}</Text>
              {focused && <View style={[styles.indicator, {backgroundColor: C.tabActive}]} />}
            </View>
          ),
        }}
      />
    </Tab.Navigator>
  );
}

function AIButton({onPress, C}: {onPress?: () => void; C: AppColors}) {
  return (
    <TouchableOpacity
      activeOpacity={0.9}
      onPress={onPress}
      style={styles.aiButtonWrapper}>
      <View style={[styles.aiButtonInner, {backgroundColor: C.accent}]}>
        <Text style={{fontSize: 22}}>🤖</Text>
        <Text style={styles.aiButtonText}>AI</Text>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  iconContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    width: 40,
    height: 40,
  },
  indicator: {
    width: 4,
    height: 4,
    borderRadius: 2,
    marginTop: 4,
    position: 'absolute',
    bottom: -10,
  },
  aiButtonWrapper: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: -12, // Hafif bir yükselti, ama barı koparmıyor
  },
  aiButtonInner: {
    width: 68,
    height: 44,
    borderRadius: 16, // Squircle hissi için orta düzey radius
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    // Premium Glow & Shadow
    shadowColor: '#3B82F6',
    shadowOffset: {width: 0, height: 4},
    shadowOpacity: 0.3,
    shadowRadius: 8,
    elevation: 6,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.2)',
  },
  aiButtonText: {
    color: '#FFF',
    fontSize: 12,
    fontWeight: '800',
    marginLeft: 4,
    letterSpacing: 0.5,
  },
});
