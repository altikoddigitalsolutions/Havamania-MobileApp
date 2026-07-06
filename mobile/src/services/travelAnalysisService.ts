import { DailyWeatherItem } from './openMeteoApi';
import { TravelType } from '../store/travelStore';
import { getWeatherLabel, getWeatherEmoji } from '../theme';
import { formatRainProbability } from '../utils/weatherUtils';

export interface TravelAnalysisResult {
  score: number;
  averageTemp: number;
  maxPrecipProbability: number | string;
  precipitationRiskText: string;
  summary: string;
  advice: string;
  icon: string;
  emoji: string;
  color: string;
  status: 'past' | 'active' | 'ready' | 'pending' | 'far-future';
  packing: string[];
  activities: string[];
  metrics?: {
    rain_sum: number;
    wind_max: number;
    uv_max: number;
    temp_min_avg: number;
  };
}

const UNKNOWN = "Bilinmiyor";

function getPrecipitationRiskText(prob: number | string, sum: number, mainWeatherCode: number): string {
  const formatted = formatRainProbability(prob);

  if (mainWeatherCode >= 80) {
    if (formatted === "bilinmiyor" || (typeof prob === 'number' && prob < 70)) return "Yüksek";
  } else if (mainWeatherCode >= 51) {
    if (formatted === "bilinmiyor" || (typeof prob === 'number' && prob < 55)) return "Orta";
  }

  if (formatted !== "bilinmiyor") return formatted;
  if (sum > 0) {
    if (sum > 10) return 'Yüksek';
    if (sum > 2) return 'Orta';
    return 'Düşük';
  }
  return UNKNOWN;
}

export function analyzeTravelPlan(
  items: DailyWeatherItem[],
  startDate: string,
  endDate: string,
  type: TravelType
): TravelAnalysisResult {
  const periodItems = items.filter(i => i.date >= startDate && i.date <= endDate);

  if (periodItems.length === 0) {
    return {
      score: 0,
      averageTemp: 0,
      maxPrecipProbability: UNKNOWN,
      precipitationRiskText: UNKNOWN,
      summary: 'Veri yok',
      advice: 'Hava durumu verisi henüz mevcut değil.',
      icon: 'help-circle-outline',
      emoji: '❓',
      color: '#94A3B8',
      status: 'pending',
      packing: ['Standart'],
      activities: []
    };
  }

  const avgMax = periodItems.reduce((acc, i) => acc + i.temp_max, 0) / periodItems.length;
  const avgMin = periodItems.reduce((acc, i) => acc + i.temp_min, 0) / periodItems.length;
  const averageTemp = Math.round(avgMax);
  const tempMinAvg = Math.round(avgMin);

  let rawMaxProb = Math.max(...periodItems.map(i => i.precipitation_probability));
  let maxPrecipSum = Math.max(...periodItems.map(i => i.precipitation_sum));
  let windMax = Math.max(...periodItems.map(i => i.wind_speed_max));
  let uvMax = Math.max(...periodItems.map(i => i.uv_index_max));

  const mainWeatherCode = periodItems[0].weather_code;
  const precipitationRiskText = getPrecipitationRiskText(rawMaxProb, maxPrecipSum, mainWeatherCode);

  let score = 100;
  let packing = ["Rahat ayakkabı", "Powerbank"];
  let activities = ["Şehir turu"];

  // Scoring logic
  let probForScore = typeof rawMaxProb === 'number' ? rawMaxProb : 0;

  if (mainWeatherCode >= 95) probForScore = Math.max(probForScore, 85);
  else if (mainWeatherCode >= 80) probForScore = Math.max(probForScore, 70);

  if (probForScore > 60) {
    score -= 40;
    packing.push("Şemsiye", "Su geçirmeyen bot", "Yağmurluk");
    activities.push("Müze ziyareti", "Kapalı alan aktiviteleri");
  } else if (probForScore > 30) {
    score -= 20;
    packing.push("Şemsiye");
    activities.push("Kısa yürüyüşler");
  }

  if (windMax > 40) {
      score -= 15;
      packing.push("Rüzgarlık");
  }

  if (uvMax > 7) {
      packing.push("Güneş kremi (50+ SPF)", "Geniş kenarlı şapka");
  }

  switch (type) {
    case 'Beach':
      if (avgMax < 24) score -= 30;
      packing.push("Mayo", "Plaj havlusu", "Güneş gözlüğü");
      activities.push("Yüzme", "Güneşlenme");
      break;
    case 'Winter':
      if (avgMax > 8) score -= 30;
      packing.push("Kalın mont", "Termal içlik", "Bere & Eldiven");
      activities.push("Kayak / Snowboard", "Sıcak içecek keyfi");
      break;
    case 'Camping':
      if (avgMin < 6) score -= 25;
      if (probForScore > 30) score -= 30;
      packing.push("Uyku tulumu", "Kafa lambası", "İlkyardım kiti");
      activities.push("Kamp ateşi", "Yıldız gözlemi");
      break;
    case 'Culture':
      activities.push("Tarihi yerler", "Yerel lezzet tadımı");
      break;
  }

  score = Math.max(0, Math.min(100, score));
  const weatherLabel = getWeatherLabel(mainWeatherCode).toLowerCase();

  let advice = `Hava genellikle ${weatherLabel}.`;
  if (probForScore > 70) advice += " Yağış riski çok yüksek, planlarını buna göre yap.";
  else if (avgMax > 30) advice += " Oldukça sıcak bir hava bekleniyor, sıvı tüketimine dikkat.";
  else if (avgMin < 5) advice += " Geceler oldukça serin geçecek, tedbirli olmalısın.";

  let color = "#10B981";
  if (score < 50) color = "#EF4444";
  else if (score < 80) color = "#F59E0B";

  return {
    score,
    averageTemp,
    maxPrecipProbability: rawMaxProb,
    precipitationRiskText,
    summary: getWeatherLabel(mainWeatherCode),
    advice,
    icon: getIoniconName(mainWeatherCode),
    emoji: getWeatherEmoji(mainWeatherCode),
    color,
    status: 'ready',
    packing,
    activities,
    metrics: {
        rain_sum: maxPrecipSum,
        wind_max: windMax,
        uv_max: uvMax,
        temp_min_avg: tempMinAvg
    }
  };
}

function getIoniconName(code: number): string {
    if (code === 0) return 'sunny-outline';
    if (code <= 2) return 'partly-sunny-outline';
    if (code === 3) return 'cloud-outline';
    if (code <= 48) return 'reorder-three-outline';
    if (code <= 99) return 'rainy-outline';
    return 'sunny-outline';
}

export interface TravelHistorySummary {
  averageTemp: number;
  rainiestDay: string;
  sunniestDay: string;
  evaluation: string;
}

export function generateTravelHistorySummary(
  items: DailyWeatherItem[],
  city: string
): TravelHistorySummary {
  if (items.length === 0) return { averageTemp: 0, rainiestDay: '-', sunniestDay: '-', evaluation: 'Veri yok.' };

  const avgTemp = Math.round(items.reduce((acc, i) => acc + i.temp_max, 0) / items.length);
  const rainiest = [...items].sort((a, b) => b.precipitation_probability - a.precipitation_probability)[0];
  const sunniest = [...items].sort((a, b) => a.precipitation_probability - b.precipitation_probability)[0];

  const formatDate = (d: string) => {
    const p = d.split('-');
    const m = ["Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"];
    return `${parseInt(p[2])} ${m[parseInt(p[1]) - 1]}`;
  };

  return {
    averageTemp: avgTemp,
    rainiestDay: formatDate(rainiest.date),
    sunniestDay: formatDate(sunniest.date),
    evaluation: `Seyahat boyunca hava genellikle ${getWeatherLabel(rainiest.weather_code).toLowerCase()} seyretti. Ortalama ${avgTemp}°C sıcaklık ile planlarınızı gerçekleştirdiniz.`
  };
}
