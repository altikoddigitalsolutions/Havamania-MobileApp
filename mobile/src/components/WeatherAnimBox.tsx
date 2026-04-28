import React, {useEffect, useRef, useMemo} from 'react';
import {StyleSheet, View, Animated} from 'react-native';
import {useThemeStore} from '../store/themeStore';

const COUNT = 24; // Nesne sayısı artırıldı (daha sık)

export const WeatherAnimBox = ({weatherCode, width, height}: {weatherCode: number; width: number; height: number}) => {
  const theme = useThemeStore(state => state.theme);

  const getEmojis = () => {
    // Eğer mevsimsel temalardan biriyse, o mevsime özel emojiyi kullan
    if (theme === 'spring') return ['🌸'];
    if (theme === 'summer') return ['☀️'];
    if (theme === 'autumn') return ['🍁']; // Sonbahar için tek tip akçaağaç yaprağı
    if (theme === 'winter') return ['❄️'];

    // Açık/Koyu tema ise hava durumuna göre emojiyi kullan
    if (weatherCode >= 51 && weatherCode <= 67) return ['🌧️', '💧']; // Yağmur
    if (weatherCode >= 71 && weatherCode <= 77) return ['❄️', '🌨️']; // Kar
    if (weatherCode >= 95) return ['⚡', '🌧️']; // Fırtına
    if (weatherCode >= 1 && weatherCode <= 3) return ['☁️', '💨']; // Bulutlu
    return ['☀️']; // Açık
  };

  const emojis = getEmojis();

  return (
    <View style={[styles.container, {width, height}]} pointerEvents="none">
      {Array.from({length: COUNT}).map((_, i) => (
        <FallingItem
          key={i}
          emoji={emojis[i % emojis.length]}
          width={width}
          height={height}
          index={i}
          theme={theme}
        />
      ))}
    </View>
  );
};

const FallingItem = ({emoji, width, height, index, theme}: any) => {
  const anim = useRef(new Animated.Value(0)).current;

  const config = useMemo(() => {
    const startX = (width / COUNT) * index + (Math.random() * 10 - 5);
    return {
      duration: 2500 + Math.random() * 3500,
      delay: Math.random() * 4000,
      startX,
      endX: startX + (Math.random() * 30 - 15),
    };
  }, [index, width]);

  useEffect(() => {
    let isMounted = true;
    const run = () => {
      if (!isMounted) return;
      anim.setValue(0);
      Animated.timing(anim, {
        toValue: 1,
        duration: config.duration,
        delay: config.delay,
        useNativeDriver: true,
      }).start(({finished}) => {
        if (finished && isMounted) run();
      });
    };
    run();
    return () => { isMounted = false; anim.stopAnimation(); };
  }, [config]);

  const translateY = anim.interpolate({
    inputRange: [0, 1],
    outputRange: [-30, height + 30],
  });

  const translateX = anim.interpolate({
    inputRange: [0, 1],
    outputRange: [config.startX, config.endX],
  });

  const opacity = anim.interpolate({
    inputRange: [0, 0.2, 0.8, 1],
    outputRange: [0, 0.8, 0.8, 0], // Görünürlük için opaklık artırıldı
  });

  return (
    <Animated.Text
      style={[
        styles.emoji,
        {
          transform: [{translateX}, {translateY}],
          opacity,
          // Açık temada ve mevsimsel temalarda belirginlik için gölge güçlendirildi
          textShadowColor: (theme === 'light' || theme === 'spring' || theme === 'summer' || theme === 'autumn' || theme === 'winter') ? 'rgba(0, 0, 0, 0.5)' : 'transparent',
          textShadowOffset: {width: 1, height: 1},
          textShadowRadius: 3,
        },
      ]}>
      {emoji}
    </Animated.Text>
  );
};

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    top: 0,
    left: 0,
    overflow: 'hidden',
    borderRadius: 16,
  },
  emoji: {
    position: 'absolute',
    fontSize: 22, // Boyut büyütüldü (16 -> 22)
  },
});
