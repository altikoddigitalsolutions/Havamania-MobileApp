import firestore from '@react-native-firebase/firestore';
import { TravelPlan } from '../store/travelStore';

const getTripsCollection = (uid: string) =>
  firestore().collection('users').doc(uid).collection('trips');

export const getTrips = async (uid: string): Promise<TravelPlan[]> => {
  const snapshot = await getTripsCollection(uid).orderBy('startDate').get();
  return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as TravelPlan));
};

export const addTrip = async (uid: string, plan: Omit<TravelPlan, 'id'>) => {
  const docRef = await getTripsCollection(uid).add({
    ...plan,
    isArchived: plan.isArchived || false,
    createdAt: firestore.FieldValue.serverTimestamp(),
  });
  return docRef.id;
};

export const updateTrip = async (uid: string, tripId: string, plan: Partial<TravelPlan>) => {
  await getTripsCollection(uid).doc(tripId).update({
    ...plan,
    updatedAt: firestore.FieldValue.serverTimestamp(),
  });
};

export const deleteTrip = async (uid: string, tripId: string) => {
  await getTripsCollection(uid).doc(tripId).delete();
};
