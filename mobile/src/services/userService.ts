import firestore from '@react-native-firebase/firestore';
import storage from '@react-native-firebase/storage';

export interface UserProfile {
  uid: string;
  name: string;
  email: string;
  bio?: string;
  photoURL?: string;
  defaultCity?: string;
  temperatureUnit?: 'C' | 'F';
  theme?: string;
  language?: string;
  assistantTone?: string;
  interests?: string[];
  notificationPreferences?: {
    severe_alert_enabled: boolean;
    daily_summary_enabled: boolean;
  };
  createdAt: number;
  updatedAt: number;
  migrationCompleted?: boolean;
}

const USERS_COLLECTION = 'users';

export const getUserProfile = async (uid: string): Promise<UserProfile | null> => {
  const doc = await firestore().collection(USERS_COLLECTION).doc(uid).get();
  if (doc.exists) {
    return doc.data() as UserProfile;
  }
  return null;
};

export const createUserProfile = async (uid: string, data: Partial<UserProfile>) => {
  const profile: UserProfile = {
    uid,
    name: data.name || '',
    email: data.email || '',
    bio: '',
    photoURL: '',
    defaultCity: '',
    createdAt: Date.now(),
    updatedAt: Date.now(),
    temperatureUnit: 'C',
    assistantTone: 'DENGELI',
    notificationPreferences: {
      severe_alert_enabled: true,
      daily_summary_enabled: true,
    },
    ...data,
  };
  await firestore().collection(USERS_COLLECTION).doc(uid).set(profile);
  return profile;
};

export const updateUserProfile = async (uid: string, data: Partial<UserProfile>) => {
  await firestore()
    .collection(USERS_COLLECTION)
    .doc(uid)
    .update({
      ...data,
      updatedAt: Date.now(),
    });
};

export const uploadProfileImage = async (uid: string, filePath: string): Promise<string> => {
  const reference = storage().ref(`profile-images/${uid}/avatar.jpg`);
  await reference.putFile(filePath);
  const url = await reference.getDownloadURL();
  await updateUserProfile(uid, { photoURL: url });
  return url;
};

// ── Favorite Locations ──
export interface FavoriteLocation {
  id: string;
  label: string;
  lat: number;
  lon: number;
  is_primary: boolean;
  is_tracking_enabled: boolean;
}

export const getFavoriteLocations = async (uid: string): Promise<FavoriteLocation[]> => {
  const snapshot = await firestore().collection(USERS_COLLECTION).doc(uid).collection('locations').get();
  return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as FavoriteLocation));
};

export const addFavoriteLocation = async (uid: string, location: Omit<FavoriteLocation, 'id'>) => {
  const docRef = await firestore().collection(USERS_COLLECTION).doc(uid).collection('locations').add(location);
  return docRef.id;
};

export const updateFavoriteLocation = async (uid: string, locationId: string, data: Partial<FavoriteLocation>) => {
  await firestore().collection(USERS_COLLECTION).doc(uid).collection('locations').doc(locationId).update(data);
};

export const deleteFavoriteLocation = async (uid: string, locationId: string) => {
  await firestore().collection(USERS_COLLECTION).doc(uid).collection('locations').doc(locationId).delete();
};
