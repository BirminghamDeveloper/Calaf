import test from 'node:test';
import assert from 'node:assert/strict';
import { ageOn, validateProfile, pairId, preferences, eligible, messageWarnings, compatibility, shareable, hasSpecialCategoryData, SPECIAL_CATEGORY_FIELDS, complimentFrom, COMPLIMENT_MAX, guardianSees } from '../src/domain.js';

test('age gate handles the eighteenth birthday and invalid calendar dates', () => {
  const today = new Date('2026-10-28T12:00:00Z');
  assert.equal(ageOn('2008-10-28', today), 18);
  assert.equal(ageOn('2008-10-29', today), 17);
  assert.throws(() => ageOn('2000-02-30', today));
  assert.throws(() => validateProfile({ birthDate: '2008-10-29' }, today));
});
test('pair IDs are symmetric and do not collide on separators', () => {
  assert.equal(pairId('a', 'b'), pairId('b', 'a'));
  assert.notEqual(pairId('a_b', 'c'), pairId('a', 'b_c'));
  assert.throws(() => pairId('a', 'a'));
});
test('filters reject inverted ranges and apply deal breakers', () => {
  assert.throws(() => preferences({ minAge: 40, maxAge: 20 }));
  assert.equal(eligible({ age: 30, smoking: 'YES' }, preferences({ nonSmokerOnly: true })), false);
});
test('missing compatibility answers never imply a perfect match', () => {
  assert.equal(compatibility({}, {}).score, null);
  assert.equal(compatibility({ smoking: 'NO' }, { smoking: 'YES' }).score, 0);
  assert.deepEqual(compatibility({ smoking: 'NO' }, { smoking: 'NO' }).strong, ['smoking']);
});
test('chat warnings flag links, contact information and money requests', () => {
  assert.deepEqual(messageWarnings('Hello'), []);
  assert.ok(messageWarnings('Send money at https://example.org').includes('MONEY_REQUEST'));
  assert.ok(messageWarnings('a@example.org').includes('PERSONAL_INFORMATION'));
});

const adult = { name: 'Amina', birthDate: '1995-02-03', city: 'London', country: 'United Kingdom' };
const today = new Date('2026-10-28T12:00:00Z');
test('new optional profile fields are validated and default to unspecified', () => {
  const clean = validateProfile(adult, today);
  assert.equal(clean.maritalStatus, 'UNSPECIFIED');
  assert.equal(clean.heightCm, 0);
  assert.equal(clean.countryOfBirth, '');
  assert.equal(clean.sensitiveConsent, false);
  const full = validateProfile({ ...adult, maritalStatus: 'DIVORCED', heightCm: 170, countryOfBirth: 'Somalia' }, today);
  assert.equal(full.maritalStatus, 'DIVORCED');
  assert.equal(full.heightCm, 170);
  assert.throws(() => validateProfile({ ...adult, maritalStatus: 'MARRIED' }, today));
  assert.throws(() => validateProfile({ ...adult, heightCm: 80 }, today));
  assert.throws(() => validateProfile({ ...adult, heightCm: 170.5 }, today));
});
test('faith and heritage are refused without explicit consent and stored with it', () => {
  const faith = { religion: 'Islam', religiosity: 'PRACTISING', prays: 'USUALLY', heritage: 'SOMALI' };
  assert.throws(() => validateProfile({ ...adult, ...faith }, today), /Consent/);
  assert.throws(() => validateProfile({ ...adult, heritage: 'SOMALI', sensitiveConsent: 'yes' }, today));
  const clean = validateProfile({ ...adult, ...faith, sensitiveConsent: true }, today);
  assert.equal(clean.heritage, 'SOMALI');
  assert.equal(clean.sensitiveConsent, true);
  assert.throws(() => validateProfile({ ...adult, sensitiveConsent: true, heritage: 'KHAKI' }, today));
});
test('special category data is blanked before sharing unless the owner consented', () => {
  const stored = { name: 'Amina', religion: 'Islam', religiosity: 'PRACTISING', prays: 'ALWAYS', heritage: 'SOMALI' };
  const hidden = shareable(stored);
  for (const field of SPECIAL_CATEGORY_FIELDS) assert.ok(!hidden[field] || hidden[field] === 'UNSPECIFIED', field);
  assert.equal(hidden.name, 'Amina');
  assert.equal(shareable({ ...stored, sensitiveConsent: true }).heritage, 'SOMALI');
  assert.equal(hasSpecialCategoryData(hidden), false);
  assert.equal(hasSpecialCategoryData(stored), true);
});
test('religion never counts towards compatibility without consent on both sides', () => {
  const a = { religion: 'Islam', sensitiveConsent: true }, b = { religion: 'Islam' };
  assert.deepEqual(compatibility(shareable(a), shareable(b)).strong, []);
  assert.deepEqual(compatibility(shareable(a), shareable({ ...b, sensitiveConsent: true })).strong, ['religion']);
});
test('there is no clan field in the validated profile', () => {
  const clean = validateProfile({ ...adult, clan: 'x', qabiil: 'y' }, today);
  assert.ok(!('clan' in clean) && !('qabiil' in clean));
});

test('new filters validate their input and default to no filtering', () => {
  const none = preferences({});
  assert.deepEqual(none.maritalStatuses, []);
  assert.equal(none.hasChildren, 'ANY');
  assert.equal(none.minHeightCm, 0);
  assert.equal(none.willingToRelocate, 'ANY');
  assert.throws(() => preferences({ maritalStatuses: ['MARRIED'] }));
  assert.throws(() => preferences({ maritalStatuses: ['UNSPECIFIED'] }));
  assert.throws(() => preferences({ heritage: 'SOMALI' }));
  assert.throws(() => preferences({ minHeightCm: 100 }));
  assert.throws(() => preferences({ minHeightCm: 190, maxHeightCm: 150 }));
  assert.throws(() => preferences({ willingToRelocate: 'OPEN' }));
  assert.deepEqual(preferences({ heritage: ['SOMALI', 'SOMALI', 'KENYAN'] }).heritage, ['SOMALI', 'KENYAN']);
});
test('new filters match like the Kotlin rules and never match unspecified fields', () => {
  const member = { age: 30, gender: 'WOMAN', country: 'UK', languages: [], smoking: 'NO', wantsChildren: 'YES',
    maritalStatus: 'NEVER_MARRIED', children: 'NO', heightCm: 168, religiosity: 'PRACTISING', prays: 'USUALLY',
    heritage: 'SOMALILAND', countryOfBirth: 'Somalia', occupation: 'Primary school teacher',
    education: 'University degree', timeline: 'WITHIN_YEAR', relocation: 'OPEN' };
  const ok = filter => eligible(member, preferences(filter));
  assert.equal(ok({}), true);
  assert.equal(ok({ maritalStatuses: ['NEVER_MARRIED'] }), true);
  assert.equal(ok({ maritalStatuses: ['DIVORCED'] }), false);
  assert.equal(ok({ hasChildren: 'NO' }), true);
  assert.equal(ok({ hasChildren: 'YES' }), false);
  assert.equal(ok({ minHeightCm: 160, maxHeightCm: 175 }), true);
  assert.equal(ok({ minHeightCm: 170 }), false);
  assert.equal(ok({ religiosity: ['PRACTISING'], prays: ['USUALLY'], heritage: ['SOMALILAND'] }), true);
  assert.equal(ok({ heritage: ['KENYAN'] }), false);
  assert.equal(ok({ countryOfBirth: 'somalia', profession: 'TEACHER', education: 'degree' }), true);
  assert.equal(ok({ profession: 'doctor' }), false);
  assert.equal(ok({ timelines: ['ONE_TO_TWO_YEARS'] }), false);
  assert.equal(ok({ willingToRelocate: 'YES' }), true);
  assert.equal(ok({ willingToRelocate: 'NO' }), false);
  const reserved = { ...member, heightCm: 0, religiosity: 'UNSPECIFIED', heritage: 'UNSPECIFIED', countryOfBirth: '' };
  assert.equal(eligible(reserved, preferences({})), true);
  assert.equal(eligible(reserved, preferences({ minHeightCm: 150 })), false);
  assert.equal(eligible(reserved, preferences({ religiosity: ['PRACTISING'] })), false);
  assert.equal(eligible(reserved, preferences({ heritage: ['SOMALI'] })), false);
  assert.equal(eligible(reserved, preferences({ countryOfBirth: 'Somalia' })), false);
});

test('a compliment is shown to the receiver only while the author like stands, with its warnings', () => {
  const pair = { choices: { a: 'LIKE' }, notes: { a: 'Salaam, lovely profile' }, noteWarnings: { a: [] } };
  assert.deepEqual(complimentFrom(pair, 'a'), { note: 'Salaam, lovely profile', warnings: [] });
  assert.equal(complimentFrom(pair, 'b'), null);
  assert.equal(complimentFrom({ ...pair, choices: { a: 'PASS' } }, 'a'), null);
  assert.equal(complimentFrom({ choices: { a: 'LIKE' } }, 'a'), null);
  assert.equal(complimentFrom(undefined, 'a'), null);
  const risky = { choices: { a: 'LIKE' }, notes: { a: 'mail me a@example.org' } };
  assert.deepEqual(complimentFrom(risky, 'a').warnings, ['PERSONAL_INFORMATION']);
  assert.equal(COMPLIMENT_MAX, 140);
});

test('a guardian sees only active, unblocked matches their member opted in to share', () => {
  const pair = { members: ['owner', 'other'], state: 'ACTIVE', blockedBy: [], guardianShares: { owner: true } };
  assert.equal(guardianSees(pair, ['owner']), 'owner');
  assert.equal(guardianSees(pair, ['someone']), null);
  assert.equal(guardianSees(pair, []), null);
  // Sharing is per member: the other member's guardian sees nothing unless that member opted in too.
  assert.equal(guardianSees(pair, ['other']), null);
  assert.equal(guardianSees({ ...pair, guardianShares: { owner: false } }, ['owner']), null);
  assert.equal(guardianSees({ ...pair, guardianShares: {} }, ['owner']), null);
  assert.equal(guardianSees({ ...pair, guardianShares: undefined }, ['owner']), null);
  assert.equal(guardianSees({ ...pair, state: 'CLOSED' }, ['owner']), null);
  assert.equal(guardianSees({ ...pair, blockedBy: ['other'] }, ['owner']), null);
  assert.equal(guardianSees(undefined, ['owner']), null);
});
