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

export interface TravelAnalysis {
  date: string; // Analizin yapıldığı tarih (YYYY-MM-DD)
  summary: string;
  tempMin: number;
  tempMax: number;
  precipProb: number;
  uvIndex: number;
  windSpeed: number;
  text: string;
}

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
  analysisHistory?: TravelAnalysis[];
  lastAnalysis?: TravelAnalysis;
}

interface TravelState {
  plans: TravelPlan[];
  addPlan: (plan: Omit<TravelPlan, 'id' | 'isArchived' | 'analysisHistory' | 'lastAnalysis'>) => string;
  updatePlan: (id: string, plan: Partial<TravelPlan>) => void;
  addAnalysis: (planId: string, analysis: TravelAnalysis) => void;
  removePlan: (id: string) => void;
  archivePlan: (id: string) => void;
  unarchivePlan: (id: string) => void;
}

export const useTravelStore = create<TravelState>((set) => ({
  plans: [],
  addPlan: (plan) => {
    const id = Math.random().toString(36).substring(7);
    set((state) => ({
      plans: [...state.plans, {
        ...plan,
        id,
        isArchived: false,
        analysisHistory: []
      }].sort(
        (a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
      )
    }));
    return id;
  },
  updatePlan: (id, updatedPlan) => set((state) => ({
    plans: state.plans.map(p => p.id === id ? { ...p, ...updatedPlan } : p).sort(
      (a, b) => new Date(a.startDate).getTime() - new Date(b.startDate).getTime()
    )
  })),
  addAnalysis: (planId, analysis) => set((state) => ({
    plans: state.plans.map(p => {
      if (p.id === planId) {
        const history = p.analysisHistory || [];
        return {
          ...p,
          lastAnalysis: analysis,
          analysisHistory: [analysis, ...history].slice(0, 10) // Son 10 analizi tut
        };
      }
      return p;
    })
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
