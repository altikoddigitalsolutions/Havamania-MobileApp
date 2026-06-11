export const UNKNOWN_WEATHER = "Bilinmiyor";

/**
 * Yağış ihtimali formatlamak için merkezi fonksiyon.
 * Asla "%Bilinmiyor" veya "%null" üretmez.
 */
export function formatRainProbability(value: any): string {
  const raw = value?.toString()?.trim();
  if (!raw) return "bilinmiyor";

  const normalized = raw.toLowerCase();
  if (
    normalized === "bilinmiyor" ||
    normalized === "unknown" ||
    normalized === "undefined" ||
    normalized === "null" ||
    normalized === "n/a"
  ) {
    return "bilinmiyor";
  }

  // Eğer zaten % işareti varsa temizleyip tekrar ekleyelim (temiz veri garantisi)
  const numberPart = raw.replace("%", "").trim();
  const number = parseInt(numberPart, 10);

  if (!isNaN(number)) {
    return `%${number}`;
  }

  return "bilinmiyor";
}

/**
 * @deprecated Use formatRainProbability instead
 */
export function formatPrecipitationProbability(prob: any): string {
  const result = formatRainProbability(prob);
  return result === "bilinmiyor" ? UNKNOWN_WEATHER : result;
}

export function getPrecipitationRiskLevel(prob: number | string, sum: number = 0): string {
  const formatted = formatRainProbability(prob);
  if (formatted !== "bilinmiyor") return formatted;

  if (sum > 0) {
    if (sum > 10) return 'Yüksek';
    if (sum > 2) return 'Orta';
    return 'Düşük';
  }

  return UNKNOWN_WEATHER;
}
