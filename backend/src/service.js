import { FieldValue, FieldPath } from 'firebase-admin/firestore';
import { randomBytes, createHash } from 'node:crypto';
import { ageOn, pairId, text, choice, preferences, eligible, validateProfile, messageWarnings, compatibility, requireThat } from './domain.js';

const now = () => Date.now();
const stamp = () => FieldValue.serverTimestamp();
const hash = value => createHash('sha256').update(value).digest('hex');
const refData = snapshot => snapshot.exists ? snapshot.data() : null;

export class CalafService {
  constructor(db, auth) { this.db = db; this.auth = auth; }
  async execute(uid, input, token = {}) {
    const actions = {
      saveProfile: () => this.saveProfile(uid, input),
      savePreferences: () => this.savePreferences(uid, input),
      saveSettings: () => this.saveSettings(uid, input),
      discover: () => this.discover(uid, input),
      interest: () => this.interest(uid, input),
      sendMessage: () => this.sendMessage(uid, input),
      markRead: () => this.markRead(uid, input),
      readNotification: () => this.readNotification(uid, input),
      unmatch: () => this.unmatch(uid, input),
      block: () => this.block(uid, input),
      report: () => this.report(uid, input),
      guardianInvite: () => this.guardianInvite(uid),
      guardianAccept: () => this.guardianAccept(uid, input),
      guardianRevoke: () => this.guardianRevoke(uid, input),
      revokeSessions: async () => {
        this.recent(token);
        await this.auth.revokeRefreshTokens(uid);
        await this.db.doc(`users/${uid}`).set({ validAfter: Math.floor(now() / 1000) }, { merge: true });
        return { ok: true };
      },
      requestDeletion: async () => { this.recent(token); return this.requestDeletion(uid); },
      exportData: () => this.exportData(uid),
    };
    requireThat(Object.hasOwn(actions, input.action), 'Unknown action.');
    const user = refData(await this.db.doc(`users/${uid}`).get());
    requireThat(!user || user.status === 'ACTIVE', 'This account is unavailable.', 'permission-denied');
    return actions[input.action]();
  }
  recent(token) {
    requireThat(Number.isFinite(token.auth_time) && now() / 1000 - token.auth_time < 300,
      'Log in again before changing account security.', 'unauthenticated');
  }
  async adult(uid) {
    const user = refData(await this.db.doc(`users/${uid}`).get());
    requireThat(user?.status === 'ACTIVE' && ageOn(user.birthDate) >= 18,
      'Complete an eligible adult profile first.', 'failed-precondition');
    return user;
  }
  async saveProfile(uid, input) {
    const clean = validateProfile(input.profile ?? {});
    const userRef = this.db.doc(`users/${uid}`), profileRef = this.db.doc(`profiles/${uid}`);
    await this.db.runTransaction(async tx => {
      const [user, existing] = await tx.getAll(userRef, profileRef);
      requireThat(!user.exists || user.data().status === 'ACTIVE', 'Account unavailable.', 'permission-denied');
      requireThat(!user.exists || user.data().birthDate === input.profile.birthDate,
        'Contact support to correct your birth date.', 'failed-precondition');
      tx.set(userRef, { birthDate: input.profile.birthDate, status: 'ACTIVE', updatedAt: stamp() }, { merge: true });
      tx.set(profileRef, { ...clean, id: uid, photos: existing.data()?.photos ?? [],
        verification: existing.data()?.verification ?? 'EMAIL_ONLY',
        visibility: existing.data()?.visibility ?? 'VISIBLE', updatedAt: stamp() });
    });
    return { ok: true };
  }
  async savePreferences(uid, input) {
    await this.adult(uid);
    await this.db.doc(`preferences/${uid}`).set(preferences(input.preferences ?? {}));
    return { ok: true };
  }
  async saveSettings(uid, input) {
    await this.adult(uid);
    const value = input.settings ?? {};
    const settings = {
      visible: value.visible !== false, readReceipts: value.readReceipts === true,
      showCity: value.showCity !== false, notifications: value.notifications !== false,
      analytics: value.analytics === true, marketing: value.marketing === true,
    };
    const batch = this.db.batch();
    batch.set(this.db.doc(`settings/${uid}`), settings);
    batch.update(this.db.doc(`profiles/${uid}`), { visibility: settings.visible ? 'VISIBLE' : 'PAUSED' });
    await batch.commit();
    return { ok: true };
  }
  async discover(uid, input) {
    await this.adult(uid);
    const [own, pref] = await this.db.getAll(this.db.doc(`profiles/${uid}`), this.db.doc(`preferences/${uid}`));
    requireThat(own.exists && own.data().visibility === 'VISIBLE', 'Resume your profile to discover people.', 'failed-precondition');
    const filter = preferences(pref.data() ?? {});
    let query = this.db.collection('profiles').where('visibility', '==', 'VISIBLE').orderBy(FieldPath.documentId()).limit(40);
    if (input.cursor) query = query.startAfter(text(input.cursor, 128, 1));
    const page = await query.get();
    const candidates = [];
    for (const document of page.docs) {
      if (document.id === uid) continue;
      const [pair, user, settings] = await this.db.getAll(this.db.doc(`pairs/${pairId(uid, document.id)}`),
        this.db.doc(`users/${document.id}`), this.db.doc(`settings/${document.id}`));
      if (!user.exists || user.data().status !== 'ACTIVE' || pair.data()?.blockedBy?.length || pair.data()?.choices?.[uid]) continue;
      const profile = { ...document.data(), age: ageOn(user.data().birthDate) };
      if (profile.age < 18 || !eligible(profile, filter)) continue;
      if (settings.data()?.showCity === false) profile.city = '';
      // Public projection is allowlisted; private DOB/email/phone never enter this response.
      const { name, age, city, country, biography, culture, languages, interests, values, gender,
        religion, occupation, education, children, wantsChildren, smoking, relocation, timeline, verification, photos } = profile;
      candidates.push({ id: document.id, name, age, city, country, biography, culture, languages, interests,
        values, gender, religion, occupation, education, children, wantsChildren, smoking, relocation,
        timeline, verification, photos, compatibility: compatibility(own.data(), profile) });
    }
    return { profiles: candidates, cursor: page.size === 40 ? page.docs.at(-1).id : null };
  }
  async interest(uid, input) {
    await this.adult(uid);
    const other = text(input.userId, 128, 1), decision = choice(input.decision, ['LIKE', 'PASS']);
    const id = pairId(uid, other), pairRef = this.db.doc(`pairs/${id}`);
    const quotaRef = this.db.doc(`quotas/${uid}_${new Date().toISOString().slice(0, 10)}`);
    let matched = false;
    await this.db.runTransaction(async tx => {
      const [pair, me, candidate, targetUser, quota, subscription] = await tx.getAll(pairRef,
        this.db.doc(`profiles/${uid}`), this.db.doc(`profiles/${other}`), this.db.doc(`users/${other}`),
        quotaRef, this.db.doc(`subscriptions/${uid}`));
      requireThat(me.data()?.visibility === 'VISIBLE' && candidate.data()?.visibility === 'VISIBLE' &&
        targetUser.data()?.status === 'ACTIVE' && ageOn(targetUser.data().birthDate) >= 18,
      'This profile is unavailable.', 'failed-precondition');
      const previous = pair.data();
      requireThat(!previous?.blockedBy?.length, 'This interaction is unavailable.', 'permission-denied');
      if (previous?.choices?.[uid]) { matched = previous.state === 'ACTIVE'; return; }
      const premium = subscription.data()?.verifiedUntil > now();
      const count = quota.data()?.likes ?? 0;
      requireThat(decision !== 'LIKE' || premium || count < 10, 'Daily interest limit reached. Try again tomorrow.', 'resource-exhausted');
      const choices = { ...(previous?.choices ?? {}), [uid]: decision };
      matched = choices[uid] === 'LIKE' && choices[other] === 'LIKE' && previous?.state !== 'CLOSED';
      tx.set(pairRef, { members: [uid, other].sort(), names: { [uid]: me.data().name, [other]: candidate.data().name },
        choices, blockedBy: [], state: matched ? 'ACTIVE' : (previous?.state ?? 'PENDING'),
        updatedAt: stamp(), ...(matched && previous?.state !== 'ACTIVE' ? { matchedAt: stamp() } : {}) }, { merge: true });
      if (decision === 'LIKE') tx.set(quotaRef, { likes: count + 1, expiresAt: new Date(now() + 172800000) });
      if (matched && previous?.state !== 'ACTIVE') {
        for (const member of [uid, other]) tx.set(this.db.doc(`notifications/${member}/items/match_${id}`),
          { category: 'MATCH', targetId: id, createdAt: stamp(), read: false });
      }
    });
    return { matched, matchId: matched ? id : null };
  }
  async activePair(tx, uid, id) {
    const reference = this.db.doc(`pairs/${id}`), document = await tx.get(reference);
    const pair = document.data();
    requireThat(pair?.members?.includes(uid) && pair.state === 'ACTIVE' && !pair.blockedBy.length,
      'This conversation is unavailable.', 'permission-denied');
    return { reference, pair };
  }
  async sendMessage(uid, input) {
    await this.adult(uid);
    const id = text(input.matchId, 64, 64), body = text(input.text, 2000, 1);
    const messageId = text(input.messageId, 64, 16);
    requireThat(/^[a-zA-Z0-9-]+$/.test(messageId), 'Invalid message ID.');
    const messageRef = this.db.doc(`pairs/${id}/messages/${messageId}`);
    await this.db.runTransaction(async tx => {
      const { pair, reference } = await this.activePair(tx, uid, id);
      const target = pair.members.find(member => member !== uid);
      const [existing, otherUser] = await tx.getAll(messageRef, this.db.doc(`users/${target}`));
      requireThat(otherUser.data()?.status === 'ACTIVE', 'Conversation unavailable.', 'permission-denied');
      if (existing.exists) { requireThat(existing.data().senderId === uid, 'Invalid message ID.'); return; }
      requireThat(now() - (pair.lastSent?.[uid] ?? 0) >= 500, 'Please slow down.', 'resource-exhausted');
      tx.set(messageRef, { senderId: uid, text: body, createdAt: stamp(), warnings: messageWarnings(body) });
      tx.update(reference, { updatedAt: stamp(), lastSent: { ...pair.lastSent, [uid]: now() },
        messageCount: (pair.messageCount ?? 0) + 1 });
      tx.set(this.db.doc(`notifications/${target}/items/message_${id}`),
        { category: 'MESSAGE', targetId: id, createdAt: stamp(), read: false });
    });
    return { id: messageId };
  }
  async markRead(uid, input) {
    const id = text(input.matchId, 64, 64);
    const messageId = text(input.messageId, 64, 16);
    requireThat(/^[a-zA-Z0-9-]+$/.test(messageId), 'Invalid message ID.');
    await this.db.runTransaction(async tx => {
      const { reference, pair } = await this.activePair(tx, uid, id);
      const setting = await tx.get(this.db.doc(`settings/${uid}`));
      const message = await tx.get(this.db.doc(`pairs/${id}/messages/${messageId}`));
      requireThat(message.exists && message.data().senderId !== uid, 'Invalid received message.');
      const seenAt = message.data().createdAt.toMillis();
      if (setting.data()?.readReceipts === true) tx.update(reference, {
        readAt: { ...pair.readAt, [uid]: Math.max(pair.readAt?.[uid] ?? 0, seenAt) }
      });
    });
    return { ok: true };
  }
  async readNotification(uid, input) {
    const id = text(input.id, 150, 1);
    requireThat(/^[a-zA-Z0-9_-]+$/.test(id), 'Invalid notification.');
    const reference = this.db.doc(`notifications/${uid}/items/${id}`);
    await this.db.runTransaction(async tx => {
      const item = await tx.get(reference);
      // Do not clear a newer alert that arrived while the previous one was open.
      if (item.exists && item.data().createdAt.toMillis() === input.createdAt) tx.update(reference, { read: true });
    });
    return { ok: true };
  }
  async unmatch(uid, input) {
    const id = text(input.matchId, 64, 64);
    await this.db.runTransaction(async tx => {
      const { reference } = await this.activePair(tx, uid, id);
      tx.update(reference, { state: 'CLOSED', updatedAt: stamp() });
    });
    return { ok: true };
  }
  async block(uid, input) {
    const other = text(input.userId, 128, 1), id = pairId(uid, other);
    await this.db.runTransaction(async tx => {
      const reference = this.db.doc(`pairs/${id}`), previous = await tx.get(reference);
      const blockedBy = [...new Set([...(previous.data()?.blockedBy ?? []), uid])];
      tx.set(reference, { members: [uid, other].sort(), blockedBy, state: 'BLOCKED', updatedAt: stamp() }, { merge: true });
    });
    return { ok: true };
  }
  async report(uid, input) {
    const other = text(input.userId, 128, 1); pairId(uid, other);
    const category = choice(input.category, ['HARASSMENT', 'IMPERSONATION', 'FAKE_PROFILE', 'UNDERAGE', 'FINANCIAL_SCAM', 'INAPPROPRIATE_CONTENT', 'OTHER']);
    const reference = this.db.collection('reports').doc();
    await reference.set({ reporter: uid, reportedUser: other, category, details: text(input.details ?? '', 2000),
      status: 'OPEN', createdAt: stamp() });
    return { reportId: reference.id };
  }
  async guardianInvite(uid) {
    await this.adult(uid);
    const invitations = await this.db.collection('guardians').where('ownerId', '==', uid).get();
    requireThat(invitations.docs.filter(doc => ['INVITED', 'ACCEPTED'].includes(doc.data().state)).length < 5,
      'Remove an existing invitation before creating another.', 'resource-exhausted');
    const code = randomBytes(24).toString('hex'), id = hash(code);
    await this.db.doc(`guardians/${id}`).set({ ownerId: uid, guardianId: null, members: [uid], state: 'INVITED',
      permissions: ['INTRODUCTION_REQUESTS'], expiresAt: now() + 7 * 86400000, createdAt: stamp() });
    return { code };
  }
  async guardianAccept(uid, input) {
    const code = text(input.code, 48, 48), reference = this.db.doc(`guardians/${hash(code)}`);
    await this.db.runTransaction(async tx => {
      const invitation = await tx.get(reference), data = invitation.data();
      requireThat(data && data.state === 'INVITED' && data.expiresAt > now() && data.ownerId !== uid,
        'This invitation is invalid or expired.', 'failed-precondition');
      tx.update(reference, { guardianId: uid, members: [data.ownerId, uid], state: 'ACCEPTED', acceptedAt: stamp() });
    });
    return { ok: true };
  }
  async guardianRevoke(uid, input) {
    const reference = this.db.doc(`guardians/${text(input.id, 64, 64)}`);
    await this.db.runTransaction(async tx => {
      const invitation = await tx.get(reference);
      requireThat(invitation.data()?.members.includes(uid), 'Invitation unavailable.', 'permission-denied');
      tx.update(reference, { state: 'REVOKED', permissions: [], updatedAt: stamp() });
    });
    return { ok: true };
  }
  async requestDeletion(uid) {
    // Immediately remove discovery access; a privileged worker must finish erasure.
    const batch = this.db.batch();
    batch.set(this.db.doc(`users/${uid}`), { status: 'DELETION_REQUESTED' }, { merge: true });
    batch.set(this.db.doc(`profiles/${uid}`), { visibility: 'PAUSED' }, { merge: true });
    batch.set(this.db.doc(`deletionRequests/${uid}`), { status: 'PENDING', createdAt: stamp() });
    await batch.commit();
    await this.auth.revokeRefreshTokens(uid);
    return { status: 'PENDING' };
  }
  async exportData(uid) {
    const collections = ['users', 'profiles', 'preferences', 'settings', 'subscriptions'];
    const documents = await this.db.getAll(...collections.map(name => this.db.doc(`${name}/${uid}`)));
    return { accountData: Object.fromEntries(documents.map((doc, index) => [collections[index], doc.data() ?? null])),
      scope: 'ACCOUNT_AND_PROFILE', generatedAt: new Date().toISOString() };
  }
}
