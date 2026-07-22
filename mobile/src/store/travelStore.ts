import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as travelService from '../services/travelService';
import { useAuthStore } from './authStore';

export type TravelType =
  | 'Business'
  | 'Vacation'
  | 'Family'
  | 'Sports'
  | 'Camping'
  | 'Culture'
  | 'Nature'
  | 'Romantic'
  | 'Gastronomy'
  | 'Beach'
  | 'Winter'
  | 'Adventure'
  | 'Photography'
  | 'Shopping'
  | 'Weekend'
  | 'Health'
  | 'Event'
  | 'RoadTrip'
  | 'Other';

export interface TravelPlan {
  id: string;
  city: string;
  lat: number;
  lon: number;
  startDate: string; // ISO format (YYYY-MM-DD)
  endDate: string;   // ISO format (YYYY-MM-DD)
  type: TravelType;
  note?: string;
  isArchived?: boolean;
}

interface TravelState {
  plans: TravelPlan[];
  loading: boolean;
  fetchPlans: () => Promise<void>;
  addPlan: (plan: Omit<TravelPlan, 'id' | 'isArchived'>) => Promise<void>;
  updatePlan: (id: string, plan: Partial<TravelPlan>) => Promise<void>;
  removePlan: (id: string) => Promise<void>;
  archivePlan: (id: string) => Promise<void>;
  unarchivePlan: (id: string) => Promise<void>;
  clearPlans: () => void;
}

export const useTravelStore = create<TravelState>()(
  persist(
    (set, get) => ({
      plans: [],
      loading: false,

      fetchPlans: async () => {
        const { user, isGuest } = useAuthStore.getState();
        if (isGuest || !user) return;

        set({ loading: true });
        try {
          const plans = await travelService.getTrips(user.uid);
          set({ plans });
        } catch (error) {
          console.error('Fetch plans error:', error);
        } finally {
          set({ loading: false });
        }
      },

      addPlan: async (plan) => {
        const { user, isGuest } = useAuthStore.getState();
        const newPlan = { ...plan, isArchived: false };

        if (isGuest || !user) {
          // Local only for guest
          set((state) => ({
            plans: [...state.plans, { ...newPlan, id: Math.random().toString(36).substring(7) }].sort(
              (a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
            )
          }));
          return;
        }

        const id = await travelService.addTrip(user.uid, newPlan);
        set((state) => ({
          plans: [...state.plans, { ...newPlan, id }].sort(
            (a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
          )
        }));
      },

      updatePlan: async (id, updatedPlan) => {
        const { user, isGuest } = useAuthStore.getState();

        if (isGuest || !user) {
          set((state) => ({
            plans: state.plans.map(p => p.id === id ? { ...p, ...updatedPlan } : p).sort(
              (a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
            )
          }));
          return;
        }

        await travelService.updateTrip(user.uid, id, updatedPlan);
        set((state) => ({
          plans: state.plans.map(p => p.id === id ? { ...p, ...updatedPlan } : p).sort(
            (a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
          )
        }));
      },

      removePlan: async (id) => {
        const { user, isGuest } = useAuthStore.getState();

        if (!isGuest && user) {
          await travelService.deleteTrip(user.uid, id);
        }

        set((state) => ({
          plans: state.plans.filter((p) => p.id !== id)
        }));
      },

      archivePlan: async (id) => {
        await get().updatePlan(id, { isArchived: true });
      },

      unarchivePlan: async (id) => {
        await get().updatePlan(id, { isArchived: false });
      },

      clearPlans: () => set({ plans: [] }),
    }),
    {
      name: 'havamania:trips',
      storage: createJSONStorage(() => AsyncStorage),
      partialize: (state) => ({ plans: state.plans }), // Sadece planları kaydet
    }
  )
);
