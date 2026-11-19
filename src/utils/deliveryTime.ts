export interface ExpectedDeliveryInfo {
  isExpress: boolean;
  labelEn: string;
  labelPa: string;
  eyebrowEn: string;
  eyebrowPa: string;
  tooltip: string;
  storeHoursLabel: string;
}

const DEFAULT_OPEN_MINUTES = 8 * 60; // 8:00 AM
const DEFAULT_CLOSE_MINUTES = 21 * 60 + 30; // 9:30 PM

function parseTimeTokenToMinutes(token: string): number | null {
  const match = /(\d{1,2})(?::(\d{2}))?\s*(am|pm)/i.exec(token.trim());
  if (!match) return null;
  let hours = parseInt(match[1], 10);
  const minutes = match[2] ? parseInt(match[2], 10) : 0;
  const meridiem = match[3].toLowerCase();

  if (Number.isNaN(hours) || Number.isNaN(minutes) || hours < 1 || hours > 12 || minutes > 59) {
    return null;
  }

  if (meridiem === 'pm' && hours !== 12) hours += 12;
  if (meridiem === 'am' && hours === 12) hours = 0;

  return hours * 60 + minutes;
}

function parseStoreHoursWindow(hoursLabel?: string): { openMin: number; closeMin: number } {
  const clean = String(hoursLabel || '').trim();
  if (!clean) {
    return { openMin: DEFAULT_OPEN_MINUTES, closeMin: DEFAULT_CLOSE_MINUTES };
  }
  const parts = clean.split(/[-–—to]+/i).filter(Boolean);
  if (parts.length >= 2) {
    const start = parseTimeTokenToMinutes(parts[0]);
    const end = parseTimeTokenToMinutes(parts[1]);
    if (start !== null && end !== null && end > start) {
      return { openMin: start, closeMin: end };
    }
  }
  return { openMin: DEFAULT_OPEN_MINUTES, closeMin: DEFAULT_CLOSE_MINUTES };
}

function getCurrentMinutesInIST(now: Date): number {
  try {
    const parts = new Intl.DateTimeFormat('en-IN', {
      timeZone: 'Asia/Kolkata',
      hour: 'numeric',
      minute: 'numeric',
      hour12: false,
    }).formatToParts(now);

    const hourPart = parts.find((p) => p.type === 'hour')?.value;
    const minutePart = parts.find((p) => p.type === 'minute')?.value;
    const h = Number(hourPart);
    const m = Number(minutePart);
    if (Number.isFinite(h) && Number.isFinite(m)) {
      return (h % 24) * 60 + m;
    }
  } catch {
    // Fallback to local clock if Intl timezone parts are unavailable
  }
  return now.getHours() * 60 + now.getMinutes();
}

/**
 * Computes the dynamic Expected Delivery Time badge state for Nav Grocery Store
 * (Sandhaur 148020, store hours 8:00 AM – 9:30 PM IST).
 *
 * Returns "15-min Express" during active store/peak hours (8:00 AM – 9:30 PM IST
 * when store is open), or "Next Day Delivery" otherwise.
 */
export function computeExpectedDeliveryInfo(
  now: Date = new Date(),
  hoursLabel = '8:00 AM – 9:30 PM',
  isStoreOpen = true
): ExpectedDeliveryInfo {
  const effectiveLabel = hoursLabel?.trim() || '8:00 AM – 9:30 PM';
  const { openMin, closeMin } = parseStoreHoursWindow(effectiveLabel);
  const currentMin = getCurrentMinutesInIST(now);

  const withinStoreHours = currentMin >= openMin && currentMin < closeMin;
  const isExpress = isStoreOpen !== false && withinStoreHours;

  if (isExpress) {
    return {
      isExpress: true,
      labelEn: '15-min Express',
      labelPa: '15-ਮਿੰਟ ਐਕਸਪ੍ਰੈੱਸ',
      eyebrowEn: 'Expected Delivery',
      eyebrowPa: 'ਅਨੁਮਾਨਿਤ ਡਿਲੀਵਰੀ',
      tooltip: `15-min Express active during store hours (${effectiveLabel} IST) in Sandhaur 148020`,
      storeHoursLabel: effectiveLabel,
    };
  }

  return {
    isExpress: false,
    labelEn: 'Next Day Delivery',
    labelPa: 'ਅਗਲੇ ਦਿਨ ਡਿਲੀਵਰੀ',
    eyebrowEn: 'Expected Delivery',
    eyebrowPa: 'ਅਨੁਮਾਨਿਤ ਡਿਲੀਵਰੀ',
    tooltip: `Store hours: ${effectiveLabel} IST · Orders placed now schedule for Next Day Delivery starting 8:00 AM`,
    storeHoursLabel: effectiveLabel,
  };
}
