import { initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { getFirestore } from 'firebase-admin/firestore';
import { CalafService } from '../src/service.js';

if (process.env.FIREBASE_AUTH_EMULATOR_HOST !== '127.0.0.1:9099' || process.env.FIRESTORE_EMULATOR_HOST !== '127.0.0.1:8080') {
  throw new Error('This seed script is restricted to localhost emulators.');
}
initializeApp({ projectId: 'demo-calaf' });
const auth = getAuth(), db = getFirestore(), service = new CalafService(db, auth);
const people = [
  ['demo-admin', 'admin@calaf.test', 'Admin (Demo)', 'MAN', '1994-06-10'],
  ['demo-hassan', 'hassan@calaf.test', 'Hassan (Demo)', 'MAN', '1994-06-10'],
  ['demo-amina', 'amina@calaf.test', 'Amina (Demo)', 'WOMAN', '1996-04-12'],
  ['demo-hodan', 'hodan@calaf.test', 'Hodan (Demo)', 'WOMAN', '1993-11-05'],
  ['demo-yusuf', 'yusuf@calaf.test', 'Yusuf (Demo)', 'MAN', '1992-02-09'],
];
for (const [uid, email, name, gender, birthDate] of people) {
  const password = uid === 'demo-admin' ? 'CalafAdmin!2026' : 'CalafDemo!2026';
  try { await auth.getUser(uid); }
  catch (error) {
    if (error.code !== 'auth/user-not-found') throw error;
    await auth.createUser({ uid, email, password, emailVerified: true });
  }
  if (uid === 'demo-admin') await auth.updateUser(uid, { password, emailVerified: true });
  await service.execute(uid, { action: 'saveProfile', profile: {
    name, gender, birthDate, city: 'London', country: 'United Kingdom', culture: 'Somali / East African',
    languages: ['Somali', 'English'], biography: 'Fictional local test profile. Interested in family, community and a serious commitment.',
    interests: ['Reading', 'Walking'], values: ['Family', 'Honesty'], religion: 'Islam', smoking: 'NO', wantsChildren: 'YES', timeline: 'ONE_TO_TWO_YEARS',
  } });
}
await service.execute('demo-amina', { action: 'interest', userId: 'demo-hassan', decision: 'LIKE' });
await service.execute('demo-hassan', { action: 'interest', userId: 'demo-amina', decision: 'LIKE' });
await service.execute('demo-admin', { action: 'interest', userId: 'demo-amina', decision: 'LIKE' });
await service.execute('demo-amina', { action: 'interest', userId: 'demo-admin', decision: 'LIKE' });
console.log('Local demo login: admin / admin in the Android debug app. This is a regular demo member, not a privileged administrator.');
console.log('Local fixtures ready. Sign in with hassan@calaf.test / CalafDemo!2026. All profiles are fictional.');
