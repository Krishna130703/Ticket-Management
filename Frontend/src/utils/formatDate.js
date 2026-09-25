export function formatDate(isoString) {
  if (!isoString) {
    return '—';
  }
  try {
    return new Date(isoString).toLocaleString();
  } catch {
    return isoString;
  }
}
