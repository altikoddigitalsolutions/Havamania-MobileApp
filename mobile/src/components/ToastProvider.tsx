import React, {createContext, useCallback, useMemo, useState} from 'react';
import {Text, View} from 'react-native';

type ToastContextValue = {
  showToast: (message: string) => void;
};

export const ToastContext = createContext<ToastContextValue>({
  showToast: () => undefined,
});

export function ToastProvider({children}: {children: React.ReactNode}): React.JSX.Element {
  const [message, setMessage] = useState<string | null>(null);

  const showToast = useCallback((newMessage: string) => {
    setMessage(newMessage);
    setTimeout(() => setMessage(null), 2500);
  }, []);

  const value = useMemo(() => ({showToast}), [showToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      {message ? (
        <View
          style={{
            position: 'absolute',
            bottom: 24,
            alignSelf: 'center',
            backgroundColor: '#111827',
            borderRadius: 8,
            paddingHorizontal: 14,
            paddingVertical: 10,
          }}>
          <Text style={{color: '#ffffff'}}>{message}</Text>
        </View>
      ) : null}
    </ToastContext.Provider>
  );
}
