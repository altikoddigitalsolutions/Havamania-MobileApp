/**
 * Turkish character normalization for search functionality.
 * Converts characters like 'ç' to 'c', 'ğ' to 'g', etc.
 * and converts to lowercase.
 */
export const normalizeText = (text: string): string => {
  return text
    .toLowerCase()
    .replace(/ı/g, 'i')
    .replace(/ş/g, 's')
    .replace(/ğ/g, 'g')
    .replace(/ü/g, 'u')
    .replace(/ö/g, 'o')
    .replace(/ç/g, 'c')
    .trim();
};
