export const fallbackImages = [
  'https://images.unsplash.com/photo-1501386761578-eac5c94b800a?auto=format&fit=crop&w=1000&q=85',
  'https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&w=1000&q=85',
  'https://images.unsplash.com/photo-1501612780327-45045538702b?auto=format&fit=crop&w=1000&q=85',
  'https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?auto=format&fit=crop&w=1000&q=85',
];

export function optimizeImageUrl(value, width = 900) {
  if (!value) return value;
  try {
    const imageUrl = new URL(value);
    if (imageUrl.hostname.endsWith('unsplash.com')) {
      imageUrl.searchParams.set('auto', 'format');
      imageUrl.searchParams.set('fit', 'crop');
      imageUrl.searchParams.set('w', String(width));
      imageUrl.searchParams.set('q', '72');
    }
    return imageUrl.toString();
  } catch {
    return value;
  }
}

export function formatDate(value) {
  if (!value) return 'Fecha por confirmar';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Fecha por confirmar';
  return new Intl.DateTimeFormat('es-ES', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
  }).format(date);
}

export function formatTime(value) {
  if (typeof value !== 'string') return '';
  const match = /^([01]\d|2[0-3]):([0-5]\d)(?::[0-5]\d(?:\.\d+)?)?$/.exec(value);
  return match ? `${match[1]}:${match[2]}` : '';
}

export function formatPrice(value) {
  return new Intl.NumberFormat('es-ES', {
    style: 'currency',
    currency: 'EUR',
  }).format(Number(value));
}
