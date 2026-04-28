import React, {useEffect, useRef} from 'react';
import {
  StyleSheet,
  View,
  Text,
  Image,
  Animated,
  ImageBackground,
  Dimensions,
} from 'react-native';
import LinearGradient from 'react-native-linear-gradient';

const {width, height} = Dimensions.get('window');

export function TravelInspiredSplash(): React.JSX.Element {
  const contentAlpha = useRef(new Animated.Value(0)).current;
  const contentScale = useRef(new Animated.Value(0.98)).current;
  const progress = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.parallel([
      Animated.timing(contentAlpha, {
        toValue: 1,
        duration: 1500,
        useNativeDriver: true,
      }),
      Animated.spring(contentScale, {
        toValue: 1,
        friction: 8,
        tension: 40,
        useNativeDriver: true,
      }),
      Animated.timing(progress, {
        toValue: 1,
        duration: 2000,
        useNativeDriver: false, // width animation requires false
      }),
    ]).start();
  }, [contentAlpha, contentScale, progress]);

  return (
    <View style={s.container}>
      <ImageBackground
        source={require('../assets/splash_travel_bg.png')}
        style={StyleSheet.absoluteFill}
        resizeMode="cover">
        <LinearGradient
          colors={[
            'rgba(0,0,0,0.25)',
            'rgba(0,0,0,0)',
            'rgba(0,0,0,0.65)',
          ]}
          locations={[0, 0.4, 1]}
          style={StyleSheet.absoluteFill}
        />

        <Animated.View
          style={[
            s.centerBlock,
            {
              opacity: contentAlpha,
              transform: [{scale: contentScale}],
            },
          ]}>
          <Image
            source={require('../assets/havamania_logo_clean.png')}
            style={s.logo}
            resizeMode="contain"
          />
          <Text style={s.brandName}>Havamania</Text>
          <Text style={s.slogan}>
            Hava durumunu akıllıca takip et,{'\n'}seyahatlerini akıllıca planla.
          </Text>
        </Animated.View>

        <Animated.View style={[s.bottomArea, {opacity: contentAlpha}]}>
          <Image
            source={require('../assets/ic_suitcase_clean.png')}
            style={s.loadingIcon}
            resizeMode="contain"
          />
          <Text style={s.loadingText}>Yükleniyor...</Text>
          <View style={s.progressTrack}>
            <Animated.View
              style={[
                s.progressBar,
                {
                  width: progress.interpolate({
                    inputRange: [0, 1],
                    outputRange: ['0%', '100%'],
                  }),
                },
              ]}>
              <LinearGradient
                colors={['#3B82F6', '#60A5FA']}
                start={{x: 0, y: 0}}
                end={{x: 1, y: 0}}
                style={StyleSheet.absoluteFill}
              />
            </Animated.View>
          </View>
        </Animated.View>
      </ImageBackground>
    </View>
  );
}

const s = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0F172A',
  },
  centerBlock: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: -80,
  },
  logo: {
    width: 110,
    height: 110,
  },
  brandName: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#FFFFFF',
    marginTop: 20,
    letterSpacing: 1,
  },
  slogan: {
    fontSize: 16,
    color: 'rgba(255, 255, 255, 0.85)',
    textAlign: 'center',
    marginTop: 12,
    lineHeight: 22,
    paddingHorizontal: 40,
  },
  bottomArea: {
    position: 'absolute',
    bottom: 70,
    left: 0,
    right: 0,
    alignItems: 'center',
  },
  loadingIcon: {
    width: 28,
    height: 28,
    opacity: 0.7,
  },
  loadingText: {
    fontSize: 12,
    color: 'rgba(255, 255, 255, 0.5)',
    marginTop: 14,
    letterSpacing: 1,
  },
  progressTrack: {
    width: 140,
    height: 2,
    backgroundColor: 'rgba(255, 255, 255, 0.15)',
    borderRadius: 1,
    marginTop: 16,
    overflow: 'hidden',
  },
  progressBar: {
    height: '100%',
  },
});
