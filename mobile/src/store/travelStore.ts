import { create } from 'zustand';

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
  addPlan: (plan: Omit<TravelPlan, 'id' | 'isArchived'>) => void;
  updatePlan: (id: string, plan: Partial<TravelPlan>) => void;
  removePlan: (id: string) => void;
  archivePlan: (id: string) => void;
  unarchivePlan: (id: string) => void;
}

export const useTravelStore = create<TravelState>((set) => ({
  plans: [],
  addPlan: (plan) => set((state) => ({
    plans: [...state.plans, { ...plan, id: Math.random().toString(36).substring(7), isArchived: false }].sort(
      (a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
    )
  })),
  updatePlan: (id, updatedPlan) => set((state) => ({
    plans: state.plans.map(p => p.id === id ? { ...p, ...updatedPlan } : p).sort(
      (a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
    )
  })),
  removePlan: (id) => set((state) => ({
    plans: state.plans.filter((p) => p.id !== id)
  })),
  archivePlan: (id) => set((state) => ({
    plans: state.plans.map(p => p.id === id ? { ...p, isArchived: true } : p)
  })),
  unarchivePlan: (id) => set((state) => ({
    plans: state.plans.map(p => p.id === id ? { ...p, isArchived: false } : p)
  })),
}));
