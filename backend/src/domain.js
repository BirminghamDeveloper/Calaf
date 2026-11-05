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
// UK GDPR special category data: religion, how practising, prayer and heritage (ethnicity). Optional, and only stored
// or shown while the member has given explicit consent. There is deliberately no clan (qabiil) field.
export const SPECIAL_CATEGORY_FIELDS = ['religion', 'religiosity', 'prays', 'heritage'];
const MARITAL = ['NEVER_MARRIED', 'DIVORCED', 'WIDOWED', 'UNSPECIFIED'];
const RELIGIOSITY = ['VERY_PRACTISING', 'PRACTISING', 'MODERATELY', 'NOT_PRACTISING', 'UNSPECIFIED'];
const PRAYS = ['ALWAYS', 'USUALLY', 'SOMETIMES', 'NEVER', 'UNSPECIFIED'];
const HERITAGE = ['SOMALI', 'SOMALILAND', 'ETHIOPIAN', 'ERITREAN', 'DJIBOUTIAN', 'KENYAN', 'SUDANESE', 'OTHER', 'UNSPECIFIED'];
export function hasSpecialCategoryData(profile) {
  return Boolean(profile.religion) || ['religiosity', 'prays', 'heritage'].some(
    field => profile[field] && profile[field] !== 'UNSPECIFIED');
}
/** A copy of the profile with special category fields blanked unless the owner consented. Use before any sharing. */
export function shareable(profile) {
  if (profile.sensitiveConsent === true) return profile;
  return { ...profile, religion: '', religiosity: 'UNSPECIFIED', prays: 'UNSPECIFIED', heritage: 'UNSPECIFIED' };
}
export function validateProfile(input, now = new Date()) {
  const age = ageOn(input.birthDate, now);
  requireThat(age >= 18, 'Calaf is for adults aged 18 and over.', 'permission-denied');
  const heightCm = input.heightCm ?? 0;
  requireThat(Number.isInteger(heightCm) && (heightCm === 0 || (heightCm >= 120 && heightCm <= 230)), 'Invalid height.');
  const sensitiveConsent = input.sensitiveConsent === true;
  const faith = {
    religion: text(input.religion ?? '', 60), religiosity: choice(input.religiosity, RELIGIOSITY, 'UNSPECIFIED'),
    prays: choice(input.prays, PRAYS, 'UNSPECIFIED'), heritage: choice(input.heritage, HERITAGE, 'UNSPECIFIED'),
  };
  requireThat(sensitiveConsent || !hasSpecialCategoryData(faith),
    'Consent is needed to save faith and heritage details.', 'failed-precondition');
  return {
    name: text(input.name, 60, 2), city: text(input.city, 100, 2), country: text(input.country, 100, 2),
    biography: text(input.biography ?? '', 500), culture: text(input.culture ?? '', 200),
    languages: tags(input.languages ?? []), interests: tags(input.interests ?? []),
    values: tags(input.values ?? []), gender: choice(input.gender, ['WOMAN', 'MAN', 'UNSPECIFIED'], 'UNSPECIFIED'),
    ...faith, sensitiveConsent, occupation: text(input.occupation ?? '', 100),
    education: text(input.education ?? '', 100),
    maritalStatus: choice(input.maritalStatus, MARITAL, 'UNSPECIFIED'), heightCm,
    countryOfBirth: text(input.countryOfBirth ?? '', 100),
    children: choice(input.children, ['YES', 'NO', 'UNSPECIFIED'], 'UNSPECIFIED'),
    wantsChildren: choice(input.wantsChildren, ['YES', 'NO', 'OPEN', 'UNSPECIFIED'], 'UNSPECIFIED'),
    smoking: choice(input.smoking, ['YES', 'NO', 'UNSPECIFIED'], 'UNSPECIFIED'),
    relocation: choice(input.relocation, ['YES', 'NO', 'OPEN', 'UNSPECIFIED'], 'UNSPECIFIED'),
    timeline: choice(input.timeline, ['WITHIN_YEAR', 'ONE_TO_TWO_YEARS', 'WHEN_READY'], 'WHEN_READY'),
    age, visibility: 'VISIBLE', verification: 'EMAIL_ONLY', photos: [],
  };
}
function selection(value, options) {
  const list = value ?? [];
  requireThat(Array.isArray(list) && list.length <= options.length && list.every(item => options.includes(item)),
    'Invalid selection.');
  return [...new Set(list)];
}
function heightBound(value) {
  const height = value ?? 0;
  requireThat(Number.isInteger(height) && (height === 0 || (height >= 120 && height <= 230)), 'Invalid height.');
  return height;
}
export function preferences(input) {
  const minAge = input.minAge ?? 18, maxAge = input.maxAge ?? 70;
  requireThat(Number.isInteger(minAge) && Number.isInteger(maxAge) && minAge >= 18 && maxAge <= 120 && minAge <= maxAge, 'Invalid age range.');
  const minHeightCm = heightBound(input.minHeightCm), maxHeightCm = heightBound(input.maxHeightCm);
  requireThat(!minHeightCm || !maxHeightCm || minHeightCm <= maxHeightCm, 'Invalid height range.');
  return { minAge, maxAge, gender: choice(input.gender, ['ANY', 'WOMAN', 'MAN'], 'ANY'),
    country: text(input.country ?? '', 100), language: text(input.language ?? '', 60),
    nonSmokerOnly: input.nonSmokerOnly === true, wantsChildren: choice(input.wantsChildren, ['ANY', 'YES', 'NO', 'OPEN'], 'ANY'),
    // Added in Task 3.2. Empty lists, 'ANY', 0 and blank text all mean no filter.
    maritalStatuses: selection(input.maritalStatuses, MARITAL.filter(value => value !== 'UNSPECIFIED')),
    hasChildren: choice(input.hasChildren, ['ANY', 'YES', 'NO'], 'ANY'), minHeightCm, maxHeightCm,
    religiosity: selection(input.religiosity, RELIGIOSITY.filter(value => value !== 'UNSPECIFIED')),
    prays: selection(input.prays, PRAYS.filter(value => value !== 'UNSPECIFIED')),
    heritage: selection(input.heritage, HERITAGE.filter(value => value !== 'UNSPECIFIED')),
    countryOfBirth: text(input.countryOfBirth ?? '', 100), profession: text(input.profession ?? '', 100),
    education: text(input.education ?? '', 100),
    timelines: selection(input.timelines, ['WITHIN_YEAR', 'ONE_TO_TWO_YEARS', 'WHEN_READY']),
    willingToRelocate: choice(input.willingToRelocate, ['ANY', 'YES', 'NO'], 'ANY') };
}
export function eligible(profile, prefs) {
  return profile.age >= prefs.minAge && profile.age <= prefs.maxAge &&
    (prefs.gender === 'ANY' || profile.gender === prefs.gender) &&
    (!prefs.country || profile.country.toLowerCase() === prefs.country.toLowerCase()) &&
    (!prefs.language || profile.languages.some(value => value.toLowerCase() === prefs.language.toLowerCase())) &&
    (!prefs.nonSmokerOnly || profile.smoking === 'NO') &&
    (prefs.wantsChildren === 'ANY' || profile.wantsChildren === prefs.wantsChildren) &&
    // Filters added in Task 3.2. A filter never matches a member who left that field unspecified.
    (!prefs.maritalStatuses?.length || prefs.maritalStatuses.includes(profile.maritalStatus)) &&
    (!prefs.hasChildren || prefs.hasChildren === 'ANY' || profile.children === prefs.hasChildren) &&
    (!prefs.minHeightCm || (profile.heightCm > 0 && profile.heightCm >= prefs.minHeightCm)) &&
    (!prefs.maxHeightCm || (profile.heightCm > 0 && profile.heightCm <= prefs.maxHeightCm)) &&
    (!prefs.religiosity?.length || prefs.religiosity.includes(profile.religiosity)) &&
    (!prefs.prays?.length || prefs.prays.includes(profile.prays)) &&
    (!prefs.heritage?.length || prefs.heritage.includes(profile.heritage)) &&
    (!prefs.countryOfBirth || (profile.countryOfBirth ?? '').toLowerCase() === prefs.countryOfBirth.toLowerCase()) &&
    (!prefs.profession || (profile.occupation ?? '').toLowerCase().includes(prefs.profession.toLowerCase())) &&
    (!prefs.education || (profile.education ?? '').toLowerCase().includes(prefs.education.toLowerCase())) &&
    (!prefs.timelines?.length || prefs.timelines.includes(profile.timeline)) &&
    relocationMatches(prefs.willingToRelocate, profile.relocation);
}
function relocationMatches(wanted, answer) {
  if (!wanted || wanted === 'ANY') return true;
  return wanted === 'YES' ? answer === 'YES' || answer === 'OPEN' : answer === 'NO';
}
export function messageWarnings(body) {
  const warnings = [];
  if (/https?:\/\/|www\./i.test(body)) warnings.push('LINK');
  if (/(?:\+?\d[\s-]?){8,}|\S+@\S+\.\S+/.test(body)) warnings.push('PERSONAL_INFORMATION');
  if (/\b(send money|bank transfer|gift card|crypto|loan|wire transfer)\b/i.test(body)) warnings.push('MONEY_REQUEST');
  return warnings;
}
/**
 * The member who shared this match with their guardian, if one of [ownerIds] (members whose guardian is the viewer)
 * did. Only an active, unblocked match can be read. Sharing is opt in per match and per member.
 */
export function guardianSees(pair, ownerIds) {
  if (!pair || pair.state !== 'ACTIVE' || pair.blockedBy?.length) return null;
  return pair.members?.find(member => ownerIds.includes(member) && pair.guardianShares?.[member] === true) ?? null;
}
/** A note sent with a like, as the receiver may see it. Only while the author's like stands. */
export function complimentFrom(pair, authorId) {
  const note = pair?.notes?.[authorId];
  if (!note || pair?.choices?.[authorId] !== 'LIKE') return null;
  return { note, warnings: pair.noteWarnings?.[authorId] ?? messageWarnings(note) };
}
export const COMPLIMENT_MAX = 140;
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
