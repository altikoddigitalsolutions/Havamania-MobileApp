import React from 'react';
import { StyleSheet, View, Text, Dimensions } from 'react-native';
import { Spacing, Radius, FontSize, AppColors } from '../theme';

// Safe import for LinearGradient
let LinearGradient: any;
try {
  LinearGradient = require('react-native-linear-gradient').default;
} catch (e) {
  LinearGradient = ({ children, colors, style }: any) => (
    <View style={[style, { backgroundColor: colors[0] }]}>{children}</View>
  );
}

const { width: SCREEN_WIDTH } = Dimensions.get('window');

interface AtmosphericWeatherCardProps {
  city: string;
  temperature: number;
  description: string;
  high: number;
  low: number;
  feelsLike: number;
  weatherCode: number;
  isDay: boolean;
  lastUpdated: string;
  C: AppColors;
}

export const AtmosphericWeatherCard: React.FC<AtmosphericWeatherCardProps> = ({
  city,
  temperature,
  description,
  high,
  low,
  feelsLike,
  weatherCode,
  isDay,
  lastUpdated,
  C,
}) => {
  const getWeatherTheme = () => {
    if (!isDay) {
      return {
        colors: ['#1A1A2E', '#16213E', '#0F3460'],
        accent: '#E94560',
        type: 'night'
      };
    }

    if (weatherCode <= 1) { // Sunny / Clear
      return {
        colors: ['#4facfe', '#00f2fe', '#f9d423'],
        accent: '#f9d423',
        type: 'sunny'
      };
    } else if (weatherCode <= 3) { // Cloudy
      return {
        colors: ['#757F9A', '#D7DDE8'],
        accent: '#ffffff',
        type: 'cloudy'
      };
    } else if (weatherCode >= 51 && weatherCode <= 67) { // Rainy
      return {
        colors: ['#2c3e50', '#4ca1af'],
        accent: '#3498db',
        type: 'rainy'
      };
    } else if (weatherCode >= 71 && weatherCode <= 77) { // Snowy
      return {
        colors: ['#83a4d4', '#b6fbff'],
        accent: '#ffffff',
        type: 'snowy'
      };
    } else if (weatherCode >= 45 && weatherCode <= 48) { // Foggy
      return {
        colors: ['#bdc3c7', '#2c3e50'],
        accent: '#ecf0f1',
        type: 'foggy'
      };
    } else { // Default / Stormy
      return {
        colors: ['#0f0c29', '#302b63', '#24243e'],
        accent: '#f1c40f',
        type: 'stormy'
      };
    }
  };

  const theme = getWeatherTheme();

  return (
    <View style={styles.outerContainer}>
      <LinearGradient
        colors={theme.colors}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={styles.card}
      >
        {/* Atmospheric Effects */}
        <View style={styles.atmosphereContainer}>
          {theme.type === 'sunny' && <View style={[styles.sunFlare, { backgroundColor: theme.accent }]} />}
          {theme.type === 'night' && <View style={styles.moonGlow} />}
          {theme.type === 'rainy' && (
            <View style={styles.rainContainer}>
              {[...Array(10)].map((_, i) => (
                <View key={i} style={[styles.rainLine, { left: i * 20, top: i * 15, opacity: 0.3 }]} />
              ))}
            </View>
          )}
          {theme.type === 'cloudy' && (
            <View style={styles.cloudContainer}>
              <View style={[styles.cloudSilhouette, { top: 20, right: -20, opacity: 0.4 }]} />
              <View style={[styles.cloudSilhouette, { bottom: -10, right: 30, opacity: 0.2, transform: [{ scale: 1.5 }] }]} />
            </View>
          )}
        </View>

        {/* Content */}
        <View style={styles.content}>
          <View>
            <Text style={styles.cityText}>{city}</Text>
            <Text style={styles.lastUpdatedText}>{lastUpdated}</Text>
          </View>

          <View style={styles.mainRow}>
            <View style={styles.tempColumn}>
              <Text style={styles.tempText}>{temperature}°</Text>
              <Text style={styles.descriptionText}>{description.toUpperCase()}</Text>
            </View>

            <View style={styles.rightInfo}>
              <View style={styles.hiLoBox}>
                <Text style={styles.hiLoText}>↑ {high}°</Text>
                <Text style={styles.hiLoText}>↓ {low}°</Text>
              </View>
              <Text style={styles.feelsLikeText}>Hissedilen {feelsLike}°</Text>
            </View>
          </View>
        </View>

        {/* Glass Overlay for premium feel */}
        <View style={styles.glassOverlay} />
      </LinearGradient>
    </View>
  );
};

const styles = StyleSheet.create({
  outerContainer: {
    marginHorizontal: Spacing.md,
    marginBottom: Spacing.md,
    borderRadius: 32,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 15 },
    shadowOpacity: 0.2,
    shadowRadius: 20,
    elevation: 10,
  },
  card: {
    height: 220,
    borderRadius: 32,
    overflow: 'hidden',
    padding: Spacing.xl,
    justifyContent: 'space-between',
  },
  glassOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
  },
  atmosphereContainer: {
    ...StyleSheet.absoluteFillObject,
    overflow: 'hidden',
  },
  content: {
    flex: 1,
    zIndex: 10,
    justifyContent: 'space-between',
  },
  cityText: {
    color: '#FFF',
    fontSize: 22,
    fontWeight: '800',
    letterSpacing: -0.5,
  },
  lastUpdatedText: {
    color: 'rgba(255, 255, 255, 0.6)',
    fontSize: 10,
    fontWeight: '700',
    marginTop: 2,
  },
  mainRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
  },
  tempColumn: {
    flex: 1,
  },
  tempText: {
    color: '#FFF',
    fontSize: 76,
    fontWeight: '300',
    lineHeight: 80,
    marginLeft: -4,
  },
  descriptionText: {
    color: '#FFF',
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 2,
    marginTop: -5,
  },
  rightInfo: {
    alignItems: 'flex-end',
    paddingBottom: 8,
  },
  hiLoBox: {
    flexDirection: 'row',
    gap: 12,
    marginBottom: 4,
  },
  hiLoText: {
    color: '#FFF',
    fontSize: 16,
    fontWeight: '600',
  },
  feelsLikeText: {
    color: 'rgba(255, 255, 255, 0.8)',
    fontSize: 12,
    fontWeight: '500',
  },
  // Atmospheric Elements
  sunFlare: {
    position: 'absolute',
    top: -50,
    right: -50,
    width: 200,
    height: 200,
    borderRadius: 100,
    opacity: 0.3,
    transform: [{ scale: 1.5 }],
  },
  moonGlow: {
    position: 'absolute',
    top: 40,
    right: 40,
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: '#FFF',
    opacity: 0.15,
    shadowColor: '#FFF',
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 1,
    shadowRadius: 40,
  },
  rainContainer: {
    ...StyleSheet.absoluteFillObject,
  },
  rainLine: {
    position: 'absolute',
    width: 1,
    height: 20,
    backgroundColor: '#FFF',
    transform: [{ rotate: '15deg' }],
  },
  cloudContainer: {
    ...StyleSheet.absoluteFillObject,
  },
  cloudSilhouette: {
    position: 'absolute',
    width: 150,
    height: 80,
    backgroundColor: '#FFF',
    borderRadius: 50,
  },
});
