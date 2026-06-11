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
}

const UNKNOWN = "Bilinmiyor";

function getPrecipitationRiskText(prob: number | string, sum: number, mainWeatherCode: number): string {
  const formatted = formatRainProbability(prob);

  // Tutarlılık Kontrolü: Fırtınalı/Sağanak ise "Düşük" olamaz
  if (mainWeatherCode >= 80) { // Sağanak, Fırtınalı, Gök gürültülü
    if (formatted === "bilinmiyor" || (typeof prob === 'number' && prob < 70)) {
      return "Yüksek";
    }
  } else if (mainWeatherCode >= 51) { // Yağmurlu
    if (formatted === "bilinmiyor" || (typeof prob === 'number' && prob < 55)) {
      return "Orta";
    }
  }

  if (formatted !== "bilinmiyor") {
    return formatted;
  }

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
      packing: ['Standart']
    };
  }

  const avgMax = periodItems.reduce((acc, i) => acc + i.temp_max, 0) / periodItems.length;
  const avgMin = periodItems.reduce((acc, i) => acc + i.temp_min, 0) / periodItems.length;
  const averageTemp = Math.round(avgMax);

  let rawMaxProb = Math.max(...periodItems.map(i => i.precipitation_probability));
  let maxPrecipSum = Math.max(...periodItems.map(i => i.precipitation_sum));

  // Most frequent weather code or first day's code
  const mainWeatherCode = periodItems[0].weather_code;

  const precipitationRiskText = getPrecipitationRiskText(rawMaxProb, maxPrecipSum, mainWeatherCode);

  let score = 100;
  let packing = ["Rahat ayakkabı"];

  // Scoring logic
  let probForScore = typeof rawMaxProb === 'number' ? rawMaxProb : 0;

  // Fırtınalı havalarda skoru ve yağış ihtimalini yukarı çek
  if (mainWeatherCode >= 95) {
     if (probForScore < 75) probForScore = 75;
  } else if (mainWeatherCode >= 80) {
     if (probForScore < 70) probForScore = 70;
  }

  if (probForScore > 60) {
    score -= 40;
    packing.push("Şemsiye", "Yağmurluk");
  } else if (probForScore > 30) {
    score -= 20;
    packing.push("Şemsiye");
  } else if (probForScore > 10) {
    score -= 5;
  }

  switch (type) {
    case 'Beach':
    case 'Vacation':
      if (avgMax < 22) score -= 30;
      else if (avgMax < 26) score -= 10;
      if (maxPrecipProbability > 20) score -= 15;
      packing.push("Güneş kremi", "Gözlük");
      if (avgMax > 28) packing.push("İnce kıyafetler");
      break;
    case 'Winter':
      if (avgMax > 10) score -= 40;
      if (avgMax > 5) score -= 20;
      packing.push("Kalın mont", "Termal kıyafet", "Eldiven");
      break;
    case 'Camping':
    case 'Nature':
      if (avgMin < 5) score -= 30;
      if (avgMax > 32) score -= 15;
      if (maxPrecipProbability > 25) score -= 25;
      packing.push("Fener", "Yedek batarya", "Bot");
      break;
    default:
      if (avgMax > 28) packing.push("Güneş gözlüğü");
      if (avgMin < 10) packing.push("Hırka / Ceket");
  }

  score = Math.max(0, Math.min(100, score));

  const weatherLabel = getWeatherLabel(mainWeatherCode).toLowerCase();

  let advice = "";
  if (mainWeatherCode >= 95) {
     advice = `Hava fırtınalı görünüyor, yağış riski yüksek.`;
  } else if (mainWeatherCode >= 80) {
     advice = `Hava sağanak yağışlı görünüyor, yağış riski yüksek.`;
  } else {
     const precipDesc = precipitationRiskText === UNKNOWN ? 'bilinmiyor' : (precipitationRiskText.startsWith('%') ? `ihtimali ${precipitationRiskText}` : `riski ${precipitationRiskText.toLowerCase()}`);
     advice = `Hava ${weatherLabel}, yağmur ${precipDesc}.`;
  }

  let color = "#10B981"; // Green

  if (score < 50) {
    color = "#EF4444"; // Red
    if (probForScore > 70) advice += " Şemsiyeni mutlaka yanına almalısın.";
  } else if (score < 80) {
    color = "#F59E0B"; // Orange
  }

  if (type === 'Beach' && avgMax >= 28 && probForScore < 10) {
    advice = "Harika plaj havası!";
  } else if (type === 'Winter' && avgMax <= 2 && probForScore > 40) {
    advice = "Tam kayak havası, kar bekleniyor!";
  }

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
    packing
  };
}

function getIoniconName(code: number): string {
    if (code === 0) return 'sunny-outline';
    if (code <= 2) return 'partly-sunny-outline';
    if (code === 3) return 'cloud-outline';
    if (code <= 48) return 'reorder-three-outline';
    if (code <= 55) return 'rainy-outline';
    if (code <= 67) return 'rainy-outline';
    if (code <= 77) return 'snow-outline';
    if (code <= 82) return 'rainy-outline';
    if (code <= 99) return 'thunderstorm-outline';
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
  if (items.length === 0) {
    return {
      averageTemp: 0,
      rainiestDay: '-',
      sunniestDay: '-',
      evaluation: 'Seyahat verisi bulunamadı.'
    };
  }

  const avgTemp = Math.round(items.reduce((acc, i) => acc + i.temp_max, 0) / items.length);

  const rainiest = [...items].sort((a, b) => b.precipitation_probability - a.precipitation_probability)[0];
  const sunniest = [...items].sort((a, b) => a.precipitation_probability - b.precipitation_probability)[0];

  const formatDate = (dateStr: string) => {
    const parts = dateStr.split('-');
    const MONTHS = ["Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"];
    return `${parseInt(parts[2])} ${MONTHS[parseInt(parts[1]) - 1]}`;
  };

  const mainWeatherCode = rainiest.weather_code;
  const weatherLabel = getWeatherLabel(mainWeatherCode).toLowerCase();
  const prob = normalizeRainProbability(rainiest.precipitation_probability);
  const precipText = getPrecipitationRiskText(prob, rainiest.precipitation_sum);
  const formattedPrecip = precipText === UNKNOWN ? 'bilinmiyor' : precipText;

  let evaluation = `Bu seyahat boyunca hava genellikle ${weatherLabel} geçti.`;

  if (avgTemp > 28) {
    evaluation = `Sıcak hava aktiviteleri için harika bir dönemdi.`;
  }

  if (typeof prob === 'number' && prob > 60) {
    evaluation = `Hava ${weatherLabel}, yağmur ihtimali ${formattedPrecip} olmasına rağmen ${city} seyahati tamamlandı.`;
  } else if (prob === UNKNOWN || (typeof prob === 'number' && prob > 0)) {
    evaluation = `Hava ${weatherLabel}, yağış riski ${formattedPrecip} olarak gözlemlendi.`;
  }

  return {
    averageTemp: avgTemp,
    rainiestDay: formatDate(rainiest.date),
    sunniestDay: formatDate(sunniest.date),
    evaluation
  };
}
