import { fetchDailyWeather, DailyWeatherItem } from './openMeteoApi';
import { askChatbot } from './chatbotApi';
import { useTravelStore, TravelPlan, TravelAnalysis } from '../store/travelStore';

/**
 * Seyahat analizi ve yönetimi servisi
 */
export const travelAnalysisService = {

  /**
   * Seyahate kaç gün kaldığını hesaplar
   */
  getDaysUntil(startDate: string): number {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const start = new Date(startDate);
    start.setHours(0, 0, 0, 0);
    const diffTime = start.getTime() - today.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  },

  /**
   * Seyahat analiz metni için giriş cümlesini hazırlar
   */
  getGreeting(daysUntil: number): string {
    if (daysUntil === 0) return "Seyahatin bugün başlıyor.";
    if (daysUntil === 1) return "Seyahatine 1 gün kaldı.";
    return `Seyahatine ${daysUntil} gün kaldı.`;
  },

  /**
   * Analiz yapılıp yapılmayacağına karar verir (15 gün kuralı)
   */
  canPerformDetailedAnalysis(daysUntil: number): boolean {
    return daysUntil <= 15;
  },

  /**
   * Gerçek AI Analizi üretir
   */
  async generateAnalysis(plan: TravelPlan): Promise<TravelAnalysis | null> {
    const daysUntil = this.getDaysUntil(plan.startDate);

    console.log(`[TravelAnalysis] Starting analysis for ${plan.city} (id: ${plan.id}), days until: ${daysUntil}`);

    // 15-day rule check: No weather analysis if more than 15 days
    if (daysUntil > 15) {
      const waitText = `Seyahatine ${daysUntil} gün kaldı. Hava tahmini güvenilir aralığın dışında olduğu için şu anda hava bazlı analiz hazırlamıyorum. Seyahatine 15 gün kaldığında hava durumuna göre detaylı seyahat analizi sunulacak.`;
      return {
        date: new Date().toISOString().split('T')[0],
        summary: "Beklemede",
        tempMin: 0,
        tempMax: 0,
        precipProb: 0,
        uvIndex: 0,
        windSpeed: 0,
        text: waitText
      };
    }

    try {
      // 2. Hava Durumu Verisini Çek (Başlangıç günü için)
      let weatherContext = "Hava durumu verisi henüz net değil.";
      let targetDay: DailyWeatherItem | undefined;

      try {
        const weatherData = await fetchDailyWeather(plan.lat, plan.lon, 16); // 16 gün tahmini
        targetDay = weatherData.items.find(i => i.date === plan.startDate) || weatherData.items[0];

        if (targetDay) {
          weatherContext = `
            Sıcaklık: ${targetDay.temp_min}°C - ${targetDay.temp_max}°C
            Yağış İhtimali: %${targetDay.precipitation_probability}
            UV İndeksi: ${targetDay.uv_index_max}
            Rüzgar: ${targetDay.wind_speed_max} km/h
          `;
        }
      } catch (weatherError) {
        console.warn(`[TravelAnalysis] Weather fetch failed for ${plan.city}, proceeding with generic analysis.`, weatherError);
      }

      // 2. Önceki Analizle Kıyasla
      const previousAnalysis = plan.lastAnalysis;
      let comparisonContext = "";

      if (previousAnalysis && targetDay && previousAnalysis.summary !== "Beklemede") {
        const tempDiff = targetDay.temp_max - previousAnalysis.tempMax;
        const precipDiff = targetDay.precipitation_probability - previousAnalysis.precipProb;

        if (Math.abs(tempDiff) >= 1) {
          comparisonContext += `Dünkü tahmine göre sıcaklık ${Math.abs(tempDiff)}° ${tempDiff > 0 ? 'artmış' : 'azalmış'} görünüyor. `;
        }
        if (precipDiff !== 0) {
          comparisonContext += `Yağış ihtimali önceki analize göre ${precipDiff > 0 ? 'arttı' : 'azaldı'}. `;
        }
      }

      // 3. AI Prompt Hazırla
      const prompt = `
        ${plan.city} şehri için ${plan.startDate} tarihinde ${plan.type} amaçlı bir seyahat planlanıyor. Seyahate ${daysUntil} gün kaldı.

        Aşağıdaki hava durumu verilerini analiz ederek kullanıcıya doğal ve samimi bir dille kısa bir seyahat analizi hazırlar mısın?

        HAVA DURUMU BİLGİSİ:
        ${weatherContext}

        KIYASLAMA BİLGİSİ:
        ${comparisonContext}

        TALİMATLAR:
        - "Seyahatine ${daysUntil} gün kaldı" cümlesiyle başla.
        - Hava koşullarına göre kıyafet ve valiz önerisi yap.
        - ${plan.type} seyahat tipine uygun aktivite öner.
        - Ton: Premium, doğal ve samimi olsun.
        - Dil: Türkçe.
        - Markdown (*, #, _, -) kullanma.
        - En fazla 4 cümle olsun.
        - Yanıtını doğrudan analiz metni olarak ver.
      `;

      // 4. AI Servisini Çağır
      console.log(`[TravelAnalysis] Requesting AI analysis for ${plan.city}...`);
      const aiResponse = await askChatbot(prompt);
      let cleanText = aiResponse.answer || aiResponse;

      if (!cleanText || typeof cleanText !== 'string' || cleanText.includes("geçerli bir soru")) {
        console.warn("[TravelAnalysis] AI returned invalid response, using local fallback.");
        throw new Error("Invalid AI response");
      }

      console.log(`[TravelAnalysis] AI analysis completed for ${plan.city}.`);

      const analysis: TravelAnalysis = {
        date: new Date().toISOString().split('T')[0],
        summary: targetDay ? (targetDay.precipitation_probability > 30 ? "Yağışlı" : "Açık") : "Belirsiz",
        tempMin: targetDay?.temp_min || 0,
        tempMax: targetDay?.temp_max || 0,
        precipProb: targetDay?.precipitation_probability || 0,
        uvIndex: targetDay?.uv_index_max || 0,
        windSpeed: targetDay?.wind_speed_max || 0,
        text: cleanText
      };

      return analysis;

    } catch (error: any) {
      console.warn(`[TravelAnalysisService] Using local fallback for ${plan.city}:`, error.message);

      const daysUntil = this.getDaysUntil(plan.startDate);
      const greeting = this.getGreeting(daysUntil);

      const fallbackText = `${greeting} ${plan.city} seyahatin için hazırlıklar devam ediyor. ${plan.type} seyahat konseptine uygun kıyafetlerini seçmeyi ve rotanı hava durumuna göre planlamayı unutma. Havamania ile keyifli bir yolculuk dileriz!`;

      return {
        date: new Date().toISOString().split('T')[0],
        summary: "Hazırlık",
        tempMin: 0,
        tempMax: 0,
        precipProb: 0,
        uvIndex: 0,
        windSpeed: 0,
        text: fallbackText
      };
    }
  },

  /**
   * Bildirimleri ve otomatik analizleri kontrol eder
   */
  async checkAndNotifyPlans(plans: TravelPlan[]) {
    const todayStr = new Date().toISOString().split('T')[0];

    for (const plan of plans) {
      if (plan.isArchived) continue;

      const daysUntil = this.getDaysUntil(plan.startDate);

      // 1. 15 Gün Bildirimi & Analizi
      if (daysUntil === 15 && (!plan.lastAnalysis || plan.lastAnalysis.summary === "Beklemede")) {
        console.log(`[Notification] 15 gün kaldı bildirimi: ${plan.city} seyahatinize 15 gün kaldı.`);
        // Analizi otomatik yap
        try {
          const analysis = await this.generateAnalysis(plan);
          if (analysis) {
            useTravelStore.getState().addAnalysis(plan.id, analysis);
          }
        } catch (e) {
          console.error(`[TravelAnalysisService] Auto-analysis failed for ${plan.city} at 15-day mark.`);
        }
      }

      // 2. Günlük Bildirim (4. kural)
      if (daysUntil <= 15 && daysUntil >= 0) {
        const content = plan.lastAnalysis && plan.lastAnalysis.summary !== "Beklemede"
          ? `${plan.city} seyahatine ${daysUntil} gün kaldı. ${plan.lastAnalysis.summary} bir hava bekleniyor.`
          : `${plan.city} seyahatine ${daysUntil} gün kaldı. Hazırlıklar başlasın!`;

        console.log(`[Notification] Günlük Bildirim (${plan.city}): ${content}`);
      }
    }
  },
};
