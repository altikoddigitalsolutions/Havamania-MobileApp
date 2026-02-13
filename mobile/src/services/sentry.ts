import * as Sentry from '@sentry/react-native';

export function initSentry(): void {
  Sentry.init({
    dsn: process.env.SENTRY_DSN_MOBILE,
    tracesSampleRate: 0.2,
    enableNative: true,
  });
}
