import {create} from 'zustand';
import { FirebaseAuthTypes } from '@react-native-firebase/auth';
import * as firebaseAuth from '../services/firebaseAuth';
import * as userService from '../services/userService';
import { UserProfile } from '../services/userService';
import { Alert } from 'react-native';
import { useTravelStore } from './travelStore';
import { useThemeStore } from './themeStore';
import { useLanguageStore } from './languageStore';
import * as travelService from '../services/travelService';
import AsyncStorage from '@react-native-async-storage/async-storage';

const STORAGE_KEYS = {
  PROFILE_IMAGE: (uid: string) => `havamania:${uid}:profileImageUri`,
  PROFILE: (uid: string) => `havamania:${uid}:profile`,
  SETTINGS: (uid: string) => `havamania:${uid}:settings`,
  TRIPS: (uid: string) => `havamania:${uid}:trips`,
  ASSISTANT: (uid: string) => `havamania:${uid}:assistant`,
};

async function migrateLocalDataToFirebase(uid: string) {
  const localPlans = useTravelStore.getState().plans;
  if (localPlans.length > 0) {
    try {
      for (const plan of localPlans) {
        // ID'yi Firebase vereceği için omit ediyoruz
        const { id, ...planData } = plan;
        await travelService.addTrip(uid, planData);
      }
      // Migration başarılı ise local'i temizle veya işaretle
      useTravelStore.getState().clearPlans();
      await useTravelStore.getState().fetchPlans();
    } catch (error) {
      console.error('Migration error:', error);
    }
  }
}

interface AuthState {
  user: FirebaseAuthTypes.User | null;
  userProfile: UserProfile | null;
  localProfileImage: string | null;
  isAuthenticated: boolean;
  isGuest: boolean;
  initializing: boolean;
  loading: boolean;

  signIn: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string, fullName: string) => Promise<void>;
  signOut: () => Promise<void>;
  loginAsGuest: () => void;
  resetPassword: (email: string) => Promise<void>;
  updateProfile: (data: Partial<UserProfile>) => Promise<void>;
  refreshProfile: () => Promise<void>;
  setLocalProfileImage: (uri: string | null) => Promise<void>;
  loadLocalProfile: () => Promise<void>;
  setUser: (user: FirebaseAuthTypes.User | null) => void;
  setInitializing: (val: boolean) => void;
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  userProfile: null,
  localProfileImage: null,
  isAuthenticated: false,
  isGuest: false,
  initializing: true,
  loading: false,

  setUser: async (user) => {
    if (user) {
      set({ user, isAuthenticated: true, isGuest: false });
      const profile = await userService.getUserProfile(user.uid);

      // UID bazlı local resmi yükle
      const localImage = await AsyncStorage.getItem(STORAGE_KEYS.PROFILE_IMAGE(user.uid));

      set({
        userProfile: profile,
        initializing: false,
        localProfileImage: profile?.photoURL || localImage
      });

      if (profile) {
        if (profile.theme) {
          useThemeStore.getState().setTheme(profile.theme as any);
        }
        if (profile.language) {
          useLanguageStore.getState().setLanguage(profile.language as any);
        }
      }

      // Giriş yapıldığında migration kontrolü
      if (profile && !profile.migrationCompleted) {
        await migrateLocalDataToFirebase(user.uid);
        await userService.updateUserProfile(user.uid, { migrationCompleted: true });
        await get().refreshProfile();
      }
    } else {
      set({ user: null, userProfile: null, isAuthenticated: false, initializing: false, localProfileImage: null });
    }
  },

  setInitializing: (val) => set({ initializing: val }),

  signIn: async (email, password) => {
    try {
      set({ loading: true });
      await firebaseAuth.signInWithEmail(email, password);
    } catch (error: any) {
      throw error;
    } finally {
      set({ loading: false });
    }
  },

  signUp: async (email, password, fullName) => {
    try {
      set({ loading: true });
      const { user } = await firebaseAuth.signUpWithEmail(email, password);
      if (user) {
        await user.updateProfile({ displayName: fullName });
        const profile = await userService.createUserProfile(user.uid, {
          name: fullName,
          email: email,
        });
        set({ userProfile: profile });
        // Migration logic
        await migrateLocalDataToFirebase(user.uid);
      }
    } catch (error: any) {
      throw error;
    } finally {
      set({ loading: false });
    }
  },

  signOut: async () => {
    const { user } = get();
    try {
      await firebaseAuth.signOut();
      useTravelStore.getState().clearPlans();
      set({
        user: null,
        userProfile: null,
        isAuthenticated: false,
        isGuest: false,
        localProfileImage: null
      });
      // Not clearing all data, just state. UID-based local data stays but is not visible.
    } catch (error) {
      console.error('Sign out error:', error);
    }
  },

  loginAsGuest: () => {
    set({ isAuthenticated: true, isGuest: true, user: null, userProfile: null });
  },

  resetPassword: async (email) => {
    await firebaseAuth.sendPasswordResetEmail(email);
  },

  updateProfile: async (data) => {
    const { user } = get();
    if (user) {
      await userService.updateUserProfile(user.uid, data);
      await get().refreshProfile();
    }
  },

  refreshProfile: async () => {
    const { user } = get();
    if (user) {
      const profile = await userService.getUserProfile(user.uid);
      const localImage = await AsyncStorage.getItem(STORAGE_KEYS.PROFILE_IMAGE(user.uid));
      set({
        userProfile: profile,
        localProfileImage: profile?.photoURL || localImage
      });
    }
  },

  setLocalProfileImage: async (uri) => {
    const { user } = get();
    if (!user) return;
    try {
      set({ localProfileImage: uri });
      const key = STORAGE_KEYS.PROFILE_IMAGE(user.uid);
      if (uri) {
        await AsyncStorage.setItem(key, uri);
      } else {
        await AsyncStorage.removeItem(key);
      }
    } catch (e) {
      console.error('Save profile image error:', e);
    }
  },

  loadLocalProfile: async () => {
    const { user } = get();
    if (!user) return;
    try {
      const uri = await AsyncStorage.getItem(STORAGE_KEYS.PROFILE_IMAGE(user.uid));
      set({ localProfileImage: uri });
    } catch (e) {
      console.error('Load profile image error:', e);
    }
  },
}));
