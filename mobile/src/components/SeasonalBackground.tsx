import React, {useEffect, useRef, useMemo} from 'react';
import {StyleSheet, View, Animated, useWindowDimensions} from 'react-native';
import {useThemeStore} from '../store/themeStore';

const COUNT = 80;

const AnimatedItem = ({emoji, index, width, height}: {emoji: string; index: number; width: number; height: number}) => {
  const anim = useRef(new Animated.Value(0)).current;

  const config = useMemo(() => {
    const sectionWidth = width / COUNT;
    const startX = sectionWidth * index + (Math.random() * 40 - 20);
    return {
      duration: 7000 + Math.random() * 9000,
      delay: Math.random() * 12000,
      startX: startX,
      endX: startX + (Math.random() * 160 - 80),
      rotateStart: `${Math.floor(Math.random() * 360)}deg`,
      rotateEnd: `${Math.floor(720 + Math.random() * 360)}deg`,
      fontSize: 18 + Math.random() * 12, // Nesne boyutu küçültüldü (önceki 30-50 arasıydı)
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
        if (finished && isMounted) {
          setTimeout(run, Math.random() * 1000);
        }
      });
    };

    run();
    return () => {
      isMounted = false;
      anim.stopAnimation();
    };
  }, [anim, config]);

  const translateX = anim.interpolate({
    inputRange: [0, 1],
    outputRange: [config.startX, config.endX],
  });

  const translateY = anim.interpolate({
    inputRange: [0, 1],
    outputRange: [-100, height + 100],
  });

  const rotate = anim.interpolate({
    inputRange: [0, 1],
    outputRange: [config.rotateStart, config.rotateEnd],
  });

  const opacity = anim.interpolate({
    inputRange: [0, 0.1, 0.9, 1],
    outputRange: [0, 0.8, 0.8, 0],
  });

  return (
    <Animated.Text
      style={[
        styles.decor,
        {
          fontSize: config.fontSize,
          transform: [
            {translateX: translateX},
            {translateY: translateY},
            {rotate: rotate}
          ],
          opacity: opacity,
        },
      ]}>
      {emoji}
    </Animated.Text>
  );
};

export const SeasonalBackground: React.FC = () => {
  const {theme, animationsEnabled} = useThemeStore();
  const {width, height} = useWindowDimensions();

  const emojis = useMemo(() => {
    if (!animationsEnabled) return [];
    switch (theme) {
      case 'winter': return ['❄️'];
      case 'spring': return ['🌸'];
      case 'summer': return ['☀️'];
      case 'autumn': return ['🍂'];
      default: return [];
    }
  }, [theme, animationsEnabled]);

  if (emojis.length === 0 || width === 0) return null;

  return (
    <View style={[StyleSheet.absoluteFill, {zIndex: -1}]} pointerEvents="none">
      {Array.from({length: COUNT}).map((_, i) => (
        <AnimatedItem
          key={`${theme}-${i}-${animationsEnabled}`}
          index={i}
          emoji={emojis[i % emojis.length]}
          width={width}
          height={height}
        />
      ))}
    </View>
  );
};

const styles = StyleSheet.create({
  decor: {
    position: 'absolute',
    textShadowColor: 'rgba(0, 0, 0, 0.3)',
    textShadowOffset: {width: 1, height: 1},
    textShadowRadius: 3,
  },
});
