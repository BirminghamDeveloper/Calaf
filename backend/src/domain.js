import { createHash } from 'node:crypto';

export class DomainError extends Error {
  constructor(code, message) { super(message); this.code = code; }
}
export function requireThat(test, message, code = 'invalid-argument') {
  if (!test) throw new DomainError(code, message);
}
export function text(value, max, min = 0) {
  requireThat(typeof value === 'string', 'Expected text.');
  const cleaned = value.trim();
  requireThat(cleaned.length >= min && cleaned.length <= max, 'Invalid text length.');
  return cleaned;
}
export function ageOn(birthDate, now = new Date()) {
  requireThat(typeof birthDate === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(birthDate), 'Invalid birth date.');
  const birth = new Date(`${birthDate}T00:00:00Z`);
  requireThat(!Number.isNaN(birth.valueOf()) && birth.toISOString().slice(0, 10) === birthDate, 'Invalid birth date.');
  const today = now.toISOString().slice(0, 10);
  let age = Number(today.slice(0, 4)) - Number(birthDate.slice(0, 4));
  if (today.slice(5) < birthDate.slice(5)) age--;
  requireThat(age >= 0 && age <= 120, 'Invalid birth date.');
  return age;
}
export function pairId(a, b) {
  requireThat(typeof a === 'string' && typeof b === 'string' && a !== b, 'Choose another member.');
  return createHash('sha256').update(JSON.stringify([a, b].sort())).digest('hex');
}
export function choice(value, options, fallback) {
  const candidate = value ?? fallback;
  requireThat(options.includes(candidate), 'Invalid choice.');
  return candidate;
}
export function tags(value) {
  requireThat(Array.isArray(value) && value.length <= 20, 'Too many selections.');
  return [...new Set(value.map(item => text(item, 60, 1)))];
}
export function validateProfile(input, now = new Date()) {
  const age = ageOn(input.birthDate, now);
  requireThat(age >= 18, 'Calaf is for adults aged 18 and over.', 'permission-denied');
  return {
    name: text(input.name, 60, 2), city: text(input.city, 100, 2), country: text(input.country, 100, 2),
    biography: text(input.biography ?? '', 500), culture: text(input.culture ?? '', 200),
    languages: tags(input.languages ?? []), interests: tags(input.interests ?? []),
    values: tags(input.values ?? []), gender: choice(input.gender, ['WOMAN', 'MAN', 'UNSPECIFIED'], 'UNSPECIFIED'),
    religion: text(input.religion ?? '', 60), occupation: text(input.occupation ?? '', 100),
    education: text(input.education ?? '', 100),
    children: choice(input.children, ['YES', 'NO', 'UNSPECIFIED'], 'UNSPECIFIED'),
    wantsChildren: choice(input.wantsChildren, ['YES', 'NO', 'OPEN', 'UNSPECIFIED'], 'UNSPECIFIED'),
    smoking: choice(input.smoking, ['YES', 'NO', 'UNSPECIFIED'], 'UNSPECIFIED'),
    relocation: choice(input.relocation, ['YES', 'NO', 'OPEN', 'UNSPECIFIED'], 'UNSPECIFIED'),
    timeline: choice(input.timeline, ['WITHIN_YEAR', 'ONE_TO_TWO_YEARS', 'WHEN_READY'], 'WHEN_READY'),
    age, visibility: 'VISIBLE', verification: 'EMAIL_ONLY', photos: [],
  };
}
export function preferences(input) {
  const minAge = input.minAge ?? 18, maxAge = input.maxAge ?? 70;
  requireThat(Number.isInteger(minAge) && Number.isInteger(maxAge) && minAge >= 18 && maxAge <= 120 && minAge <= maxAge, 'Invalid age range.');
  return { minAge, maxAge, gender: choice(input.gender, ['ANY', 'WOMAN', 'MAN'], 'ANY'),
    country: text(input.country ?? '', 100), language: text(input.language ?? '', 60),
    nonSmokerOnly: input.nonSmokerOnly === true, wantsChildren: choice(input.wantsChildren, ['ANY', 'YES', 'NO', 'OPEN'], 'ANY') };
}
export function eligible(profile, prefs) {
  return profile.age >= prefs.minAge && profile.age <= prefs.maxAge &&
    (prefs.gender === 'ANY' || profile.gender === prefs.gender) &&
    (!prefs.country || profile.country.toLowerCase() === prefs.country.toLowerCase()) &&
    (!prefs.language || profile.languages.some(value => value.toLowerCase() === prefs.language.toLowerCase())) &&
    (!prefs.nonSmokerOnly || profile.smoking === 'NO') &&
    (prefs.wantsChildren === 'ANY' || profile.wantsChildren === prefs.wantsChildren);
}
export function messageWarnings(body) {
  const warnings = [];
  if (/https?:\/\/|www\./i.test(body)) warnings.push('LINK');
  if (/(?:\+?\d[\s-]?){8,}|\S+@\S+\.\S+/.test(body)) warnings.push('PERSONAL_INFORMATION');
  if (/\b(send money|bank transfer|gift card|crypto|loan|wire transfer)\b/i.test(body)) warnings.push('MONEY_REQUEST');
  return warnings;
}
export function compatibility(a, b) {
  const strong = [], differences = [];
  for (const field of ['timeline', 'wantsChildren', 'smoking', 'relocation', 'religion']) {
    if (!a[field] || !b[field] || a[field] === 'UNSPECIFIED' || b[field] === 'UNSPECIFIED') continue;
    (a[field] === b[field] ? strong : differences).push(field);
  }
  for (const field of ['languages', 'values', 'interests']) {
    if (a[field]?.length && b[field]?.length) {
      (a[field].some(value => b[field].includes(value)) ? strong : differences).push(field);
    }
  }
  const considered = strong.length + differences.length;
  return { score: considered ? Math.round(100 * strong.length / considered) : null, strong, differences, considered };
}
