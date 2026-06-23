import i18next from 'i18next';

/**
 * Tarihleri doğal dile çevirir (Örn: "Bugün 14:00", "Dün 09:41", "5 saat önce")
 */
export function formatNaturalDate(isoString: string | undefined): string {
  if (!isoString) return '';
  const date = new Date(isoString);
  const now = new Date();
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);

  if (diffInSeconds < 60) return i18next.t('date.justNow') || 'Az önce';
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)} ${i18next.t('date.minutesAgo') || 'dakika önce'}`;

  const isToday = date.toDateString() === now.toDateString();
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  const isYesterday = date.toDateString() === yesterday.toDateString();

  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const timeStr = `${hours}:${minutes}`;

  if (isToday) {
    if (diffInSeconds < 86400 && diffInSeconds > 0) {
        return `${Math.floor(diffInSeconds / 3600)} ${i18next.t('date.hoursAgo') || 'saat önce'}`;
    }
    return `${i18next.t('date.today') || 'Bugün'} ${timeStr}`;
  }

  if (isYesterday) return `${i18next.t('date.yesterday') || 'Dün'} ${timeStr}`;

  return date.toLocaleDateString(i18next.language, {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/**
 * 00:00 saatlerini temizler, sadece tarih lazımsa tarih döndürür.
 */
export function formatSafeEventTime(isoString: string | undefined): string {
    if (!isoString) return '';
    const date = new Date(isoString);
    const hours = date.getHours();
    const minutes = date.getMinutes();

    if (hours === 0 && minutes === 0) {
        return date.toLocaleDateString(i18next.language, {
            day: 'numeric',
            month: 'long',
            weekday: 'long',
        });
    }

    return date.toLocaleString(i18next.language, {
        day: 'numeric',
        month: 'short',
        hour: '2-digit',
        minute: '2-digit',
    });
}
