import test from 'node:test';
import assert from 'node:assert/strict';
import { initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { CalafService } from '../src/service.js';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc } from 'firebase/firestore';
import { readFile } from 'node:fs/promises';
import { Timestamp } from 'firebase-admin/firestore';
import { ref, uploadBytes, getBytes, deleteObject } from 'firebase/storage';

if (!process.env.FIRESTORE_EMULATOR_HOST) throw new Error('Run only through the Firebase emulator test command.');
initializeApp({ projectId: 'demo-calaf-tests' });
const db = getFirestore(), service = new CalafService(db, { revokeRefreshTokens: async () => {} });
const profile = name => ({ name, birthDate: '1995-01-01', city: 'London', country: 'UK' });
test('mutual interest is idempotent; block revokes chat; strangers cannot read/send via service', async () => {
  const prefix = `test-${Date.now()}`, a = `${prefix}-a`, b = `${prefix}-b`, c = `${prefix}-c`;
  for (const uid of [a, b, c]) await service.execute(uid, { action: 'saveProfile', profile: profile(uid) });
  const first = await service.execute(a, { action: 'interest', userId: b, decision: 'LIKE' });
  assert.equal(first.matched, false);
  const mutual = await service.execute(b, { action: 'interest', userId: a, decision: 'LIKE' });
  const duplicate = await service.execute(b, { action: 'interest', userId: a, decision: 'LIKE' });
  assert.equal(mutual.matchId, duplicate.matchId);
  const message = { action: 'sendMessage', matchId: mutual.matchId, messageId: 'message-0000000001', text: 'Hello' };
  await assert.rejects(service.execute(c, message));
  await service.execute(a, message);
  await service.execute(a, message);
  assert.equal((await db.collection(`pairs/${mutual.matchId}/messages`).get()).size, 1);
  await service.execute(b, { action: 'block', userId: a });
  await assert.rejects(service.execute(a, { ...message, messageId: 'message-0000000002' }));
});
test('guardian invitations are consent-based and cannot be accepted twice', async () => {
  const owner = `guardian-owner-${Date.now()}`;
  await service.execute(owner, { action: 'saveProfile', profile: profile('Owner') });
  const invite = await service.execute(owner, { action: 'guardianInvite' });
  await assert.rejects(service.execute(owner, { action: 'guardianAccept', code: invite.code }));
  await service.execute('guardian-user', { action: 'guardianAccept', code: invite.code });
  await assert.rejects(service.execute('another-user', { action: 'guardianAccept', code: invite.code }));
});
test('rules deny forged profiles and premium status; blocks remove direct message reads', async () => {
  const env = await initializeTestEnvironment({ projectId: 'demo-calaf-tests', firestore: { rules: await readFile('firestore.rules', 'utf8') } });
  try {
    await db.doc('users/rules-a').set({ status: 'ACTIVE' });
    await db.doc('pairs/rules-pair').set({ members: ['rules-a', 'rules-b'], state: 'ACTIVE', blockedBy: [] });
    await db.doc('pairs/rules-pair/messages/message').set({ text: 'Private' });
    const member = env.authenticatedContext('rules-a', { email_verified: true }).firestore();
    const stranger = env.authenticatedContext('rules-c', { email_verified: true }).firestore();
    await assertFails(setDoc(doc(member, 'profiles/rules-a'), { age: 30 }));
    await assertFails(setDoc(doc(member, 'subscriptions/rules-a'), { verifiedUntil: Date.now() + 86400000 }));
    await assertFails(getDoc(doc(stranger, 'pairs/rules-pair/messages/message')));
    await assertSucceeds(getDoc(doc(member, 'pairs/rules-pair/messages/message')));
    await db.doc('pairs/rules-pair').update({ state: 'BLOCKED', blockedBy: ['rules-b'] });
    await assertFails(getDoc(doc(member, 'pairs/rules-pair/messages/message')));
  } finally { await env.cleanup(); }
});

let sequence = 0;
async function members() {
  const prefix = `expanded-${Date.now()}-${sequence++}`;
  const a = `${prefix}-a`, b = `${prefix}-b`;
  for (const uid of [a, b]) await service.execute(uid, { action: 'saveProfile', profile: profile(uid) });
  await service.execute(a, { action: 'interest', userId: b, decision: 'LIKE' });
  const { matchId } = await service.execute(b, { action: 'interest', userId: a, decision: 'LIKE' });
  return { a, b, matchId };
}
test('server rejects underage profiles and immutable DOB changes', async () => {
  const { a } = await members();
  await assert.rejects(service.execute(a, { action: 'saveProfile', profile: { ...profile(a), birthDate: '1996-01-01' } }));
  await assert.rejects(service.execute(`${a}-child`, { action: 'saveProfile', profile: { ...profile('Child'), birthDate: '2020-01-01' } }));
});
test('discovery honours privacy, filters and excludes private data', async () => {
  const { a, b } = await members();
  const c = `${a}-candidate`;
  await service.execute(c, { action: 'saveProfile', profile: { ...profile('Candidate'), country: a, wantsChildren: 'YES' } });
  await service.execute(c, { action: 'saveSettings', settings: { showCity: false } });
  await service.execute(a, { action: 'savePreferences', preferences: { country: a, wantsChildren: 'YES' } });
  let cursor = null, found;
  do {
    const page = await service.execute(a, { action: 'discover', cursor });
    found ??= page.profiles.find(p => p.id === c);
    assert.ok(page.profiles.every(p => !('birthDate' in p) && !('email' in p) && p.id !== b));
    cursor = page.cursor;
  } while (cursor);
  assert.ok(found);
  assert.ok(!found.city);
  await service.execute(c, { action: 'saveSettings', settings: { visible: false } });
  await assert.rejects(service.execute(c, { action: 'discover' }));
});
test('read receipts are opt-in, monotonic and reject invented messages', async () => {
  const { a, b, matchId } = await members();
  const messageId = 'receipt-message-0001';
  await service.execute(a, { action: 'sendMessage', matchId, messageId, text: 'Hello' });
  await service.execute(b, { action: 'markRead', matchId, messageId });
  assert.equal((await db.doc(`pairs/${matchId}`).get()).data().readAt, undefined);
  await service.execute(b, { action: 'saveSettings', settings: { readReceipts: true } });
  await service.execute(b, { action: 'markRead', matchId, messageId });
  const seen = (await db.doc(`pairs/${matchId}`).get()).data().readAt[b];
  assert.ok(seen > 0);
  await assert.rejects(service.execute(b, { action: 'markRead', matchId, messageId: 'invented-message-0001' }));
  await assert.rejects(service.execute(a, { action: 'markRead', matchId, messageId }));
  await db.doc(`pairs/${matchId}/messages/older-message-0001`).set({ senderId: a, text: 'Earlier', createdAt: Timestamp.fromMillis(seen - 1000) });
  await service.execute(b, { action: 'markRead', matchId, messageId: 'older-message-0001' });
  assert.equal((await db.doc(`pairs/${matchId}`).get()).data().readAt[b], seen);
});
test('notification read cannot clear a newer alert or another member alert', async () => {
  const { a, b, matchId } = await members();
  const id = `message_${matchId}`, reference = db.doc(`notifications/${b}/items/${id}`);
  await reference.set({ read: false, createdAt: Timestamp.fromMillis(2000) });
  await service.execute(b, { action: 'readNotification', id, createdAt: 1000 });
  assert.equal((await reference.get()).data().read, false);
  await service.execute(a, { action: 'readNotification', id, createdAt: 2000 });
  assert.equal((await reference.get()).data().read, false);
  await service.execute(b, { action: 'readNotification', id, createdAt: 2000 });
  assert.equal((await reference.get()).data().read, true);
  await assert.rejects(service.execute(b, { action: 'readNotification', id: '../other' }));
});
test('unmatch revokes sending and reports remain owned by reporter', async () => {
  const { a, b, matchId } = await members();
  const result = await service.execute(a, { action: 'report', userId: b, category: 'HARASSMENT', details: 'Example report' });
  assert.equal((await db.doc(`reports/${result.reportId}`).get()).data().reporter, a);
  await assert.rejects(service.execute(a, { action: 'report', userId: b, category: 'INVALID' }));
  await service.execute(a, { action: 'unmatch', matchId });
  await assert.rejects(service.execute(b, { action: 'sendMessage', matchId, messageId: 'unmatched-message-01', text: 'Hello' }));
});
test('guardian revocation clears permissions and strangers cannot revoke', async () => {
  const { a, b } = await members();
  const { code } = await service.execute(a, { action: 'guardianInvite' });
  await service.execute(b, { action: 'guardianAccept', code });
  const invitations = await db.collection('guardians').where('ownerId', '==', a).get();
  const id = invitations.docs[0].id;
  await assert.rejects(service.execute('stranger', { action: 'guardianRevoke', id }));
  await service.execute(b, { action: 'guardianRevoke', id });
  assert.deepEqual((await db.doc(`guardians/${id}`).get()).data().permissions, []);
});
test('export is scoped and deletion requires recent auth then suspends account', async () => {
  const { a } = await members();
  const exported = await service.execute(a, { action: 'exportData' });
  assert.equal(exported.scope, 'ACCOUNT_AND_PROFILE');
  assert.equal(exported.accountData.profiles.name, a);
  await assert.rejects(service.execute(a, { action: 'requestDeletion' }, { auth_time: 0 }));
  await service.execute(a, { action: 'requestDeletion' }, { auth_time: Math.floor(Date.now() / 1000) });
  assert.equal((await db.doc(`deletionRequests/${a}`).get()).data().status, 'PENDING');
  assert.equal((await db.doc(`profiles/${a}`).get()).data().visibility, 'PAUSED');
  await assert.rejects(service.execute(a, { action: 'discover' }));
});
test('storage rules restrict ownership, content type, size and verification', async () => {
  const env = await initializeTestEnvironment({ projectId: 'demo-calaf-tests', storage: {
    host: '127.0.0.1', port: 9199, rules: await readFile('storage.rules', 'utf8') } });
  try {
    const owner = env.authenticatedContext('photo-owner', { email_verified: true }).storage();
    const stranger = env.authenticatedContext('photo-stranger', { email_verified: true }).storage();
    const unverified = env.authenticatedContext('photo-owner', { email_verified: false }).storage();
    const path = 'privateUploads/photo-owner/photo-0.jpg';
    await assertFails(uploadBytes(ref(owner, 'privateUploads/photo-owner/photo-6.jpg'), new Uint8Array(10), { contentType: 'image/jpeg' }));
    await assertFails(uploadBytes(ref(unverified, path), new Uint8Array(10), { contentType: 'image/jpeg' }));
    await assertFails(uploadBytes(ref(owner, path), new Uint8Array(10), { contentType: 'text/plain' }));
    await assertFails(uploadBytes(ref(owner, path), new Uint8Array(5 * 1024 * 1024), { contentType: 'image/jpeg' }));
    await assertSucceeds(uploadBytes(ref(owner, path), new Uint8Array(10), { contentType: 'image/jpeg' }));
    await assertFails(getBytes(ref(stranger, path)));
    await assertSucceeds(getBytes(ref(owner, path)));
    await assertSucceeds(deleteObject(ref(owner, path)));
  } finally { await env.cleanup(); }
});
