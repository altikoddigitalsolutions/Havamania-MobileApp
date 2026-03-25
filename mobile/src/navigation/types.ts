// ── Auth Stack ────────────────────────────────────────────────────────────────
export type AuthStackParamList = {
  Login: undefined;
  SignUp: undefined;
};

// ── Alt Sekmeler (3 tab) ──────────────────────────────────────────────────────
export type MainTabParamList = {
  Weather: undefined;
  AIChat: undefined;
  Profile: undefined;
};

// ── Ana Stack (Tab + push ekranlar) ──────────────────────────────────────────
export type MainStackParamList = {
  Tabs: undefined;        // Tab navigator'ı saran kapsayıcı
  Forecast: {
    lat: number;
    lon: number;
    city: string;
  };
  Hourly: {
    lat: number;
    lon: number;
    city: string;
  };
  WeatherDetail: {
    lat: number;
    lon: number;
  };
  Alerts: undefined;
  Map: undefined;
};
