import { useWindowDimensions, PixelRatio, Platform } from 'react-native';

/**
 * Havamania Responsive Design System
 *
 * Screen Classes:
 * - compact: width < 360 (Small phones like SE)
 * - phone: 360 - 599 (Standard phones)
 * - tablet: >= 600 (Tablets and large foldables)
 */

export const Breakpoints = {
  compact: 360,
  tablet: 600,
  largeTablet: 900,
};

export interface ResponsiveConfig {
  width: number;
  height: number;
  isCompact: boolean;
  isPhone: boolean;
  isTablet: boolean;
  isLargeTablet: boolean;
  isLandscape: boolean;
  scale: number;
  fontScale: number;
}

export function useResponsive(): ResponsiveConfig {
  const { width, height, fontScale } = useWindowDimensions();

  const isCompact = width < Breakpoints.compact;
  const isTablet = width >= Breakpoints.tablet;
  const isLargeTablet = width >= Breakpoints.largeTablet;
  const isPhone = !isTablet && !isCompact;
  const isLandscape = width > height;

  // Scale factor based on a standard 375pt width (iPhone 11/12/13/14/15 size)
  const scale = width / 375;

  return {
    width,
    height,
    isCompact,
    isPhone,
    isTablet,
    isLargeTablet,
    isLandscape,
    scale,
    fontScale,
  };
}

/**
 * Responsive Spacing Helper
 * Adjusts spacing based on screen class
 */
export function getResponsiveSpacing(isTablet: boolean, isCompact: boolean) {
  return {
    xs: isCompact ? 2 : (isTablet ? 6 : 4),
    sm: isCompact ? 6 : (isTablet ? 12 : 8),
    md: isCompact ? 12 : (isTablet ? 20 : 16),
    lg: isCompact ? 18 : (isTablet ? 32 : 24),
    xl: isCompact ? 24 : (isTablet ? 48 : 32),
    xxl: isCompact ? 32 : (isTablet ? 64 : 48),
    pagePadding: isCompact ? 12 : (isTablet ? 40 : 20),
  };
}

/**
 * Responsive Font Size Helper
 * Scales font size with a cap to maintain readability
 */
export function getResponsiveFontSize(size: number, isTablet: boolean, isCompact: boolean) {
  const multiplier = isTablet ? 1.2 : (isCompact ? 0.9 : 1);
  return Math.round(size * multiplier);
}

/**
 * Layout Constants
 */
export const Layout = {
  maxContentWidth: 800,
  tabletSidebarWidth: 300,
};
