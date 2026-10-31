import test from 'node:test';
import assert from 'node:assert/strict';
import { ageOn, validateProfile, pairId, preferences, eligible, messageWarnings, compatibility } from '../src/domain.js';

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
