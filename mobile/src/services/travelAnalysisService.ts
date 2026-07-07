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
  localTips: string[];
  comparison?: string;
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
  type: TravelType,
  city: string
): TravelAnalysisResult {
  const periodItems = items.filter(i => i.date >= startDate && i.date <= endDate);
  const daysUntilStart = Math.ceil((new Date(startDate).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24));

  if (periodItems.length === 0) {
    return {
      score: 0,
      averageTemp: 0,
      maxPrecipProbability: UNKNOWN,
      precipitationRiskText: UNKNOWN,
      summary: 'Veri yok',
      advice: daysUntilStart > 15 ? 'Seyahatine 15 günden fazla var. Tahminler yaklaştıkça burası güncellenecek.' : 'Hava durumu verisi henüz mevcut değil.',
      icon: 'help-circle-outline',
      emoji: '❓',
      color: '#94A3B8',
      status: daysUntilStart > 15 ? 'far-future' : 'pending',
      packing: ['Standart'],
      activities: [],
      localTips: []
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
  let packing = ["Rahat ayakkabı", "Powerbank", "Kişisel bakım kiti"];
  let activities = ["Şehir turu", "Yerel lezzet tadımı"];
  let localTips = [`${city} seyahatinde güncel hava durumuna göre planlarını esnek tutman faydalı olabilir.`];

  // Scoring logic
  let probForScore = typeof rawMaxProb === 'number' ? rawMaxProb : 0;

  if (mainWeatherCode >= 95) probForScore = Math.max(probForScore, 85);
  else if (mainWeatherCode >= 80) probForScore = Math.max(probForScore, 70);

  if (probForScore > 60) {
    score -= 40;
    packing.push("Şemsiye", "Su geçirmeyen ayakkabı", "Yağmurluk");
    activities.push("Müze ziyareti", "Kapalı çarşı gezisi");
    localTips.push("Yağışlı havalarda kapalı mekanları tercih etmen konforunu artıracaktır.");
  } else if (probForScore > 30) {
    score -= 20;
    packing.push("Kompakt şemsiye");
    activities.push("Kısa sahil yürüyüşleri");
  }

  if (windMax > 40) {
      score -= 15;
      packing.push("Rüzgar kesici ceket");
  }

  if (uvMax > 7) {
      packing.push("Güneş kremi (50+ SPF)", "Geniş kenarlı şapka", "Güneş gözlüğü");
  }

  switch (type) {
    case 'Beach':
      if (avgMax < 24) score -= 30;
      packing.push("Mayo", "Plaj havlusu", "Terlik");
      activities.push("Yüzme", "Güneşlenme", "Tekne turu");
      localTips.push("Akşamları deniz meltemi serinletebilir, yanına ince bir hırka almalısın.");
      break;
    case 'Winter':
      if (avgMax > 8) score -= 30;
      packing.push("Kalın mont", "Termal içlik", "Bere & Eldiven", "Kalın çorap");
      activities.push("Kayak / Snowboard", "Sıcak çikolata keyfi");
      localTips.push("Kar yağışı durumunda ulaşım süreleri uzayabilir, planlarını erken yap.");
      break;
    case 'Camping':
      if (avgMin < 6) score -= 25;
      if (probForScore > 30) score -= 30;
      packing.push("4 mevsim uyku tulumu", "Kafa lambası", "Yedek pil", "Su arıtma tableti");
      activities.push("Kamp ateşi", "Gece yıldız gözlemi", "Hafif hiking");
      localTips.push("Zemin nemli olabilir, çadır altlığı kullanmayı unutma.");
      break;
    case 'Culture':
      activities.push("Tarihi saray ziyareti", "Sanat galerileri");
      packing.push("Yürüyüş ayakkabısı");
      localTips.push("Popüler yerler için biletlerini önceden online alarak vakit kazanabilirsin.");
      break;
  }

  score = Math.max(0, Math.min(100, score));
  const weatherLabel = getWeatherLabel(mainWeatherCode).toLowerCase();

  let advice = `Seyahat boyunca hava genellikle ${weatherLabel}.`;
  if (probForScore > 70) advice += " Yağış riski çok yüksek, iç mekan planlarını önceliklendir.";
  else if (avgMax > 30) advice += " Oldukça sıcak bir hava bekleniyor, güneşin dik geldiği saatlere dikkat.";
  else if (avgMin < 5) advice += " Geceler dondurucu olabilir, konaklama yerinde ısıtma olduğundan emin ol.";

  if (daysUntilStart <= 3) {
      advice = "⚠️ Seyahatine çok az kaldı! " + advice + " Tahminler şu an oldukça net.";
  }

  let color = "#10B981";
  if (score < 50) color = "#EF4444";
  else if (score < 80) color = "#F59E0B";

  // Mock comparison for demonstration
  const comparison = daysUntilStart < 7
    ? "Önceki analize göre sıcaklıklar 2 derece düştü, yağış riski ise aynı kalıyor."
    : "Tahminler henüz başlangıç aşamasında, stabil görünüyor.";

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
    packing: [...new Set(packing)], // Unique items
    activities: [...new Set(activities)],
    localTips: [...new Set(localTips)],
    comparison,
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
