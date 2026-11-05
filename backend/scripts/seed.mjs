import { initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { getFirestore } from 'firebase-admin/firestore';
import { CalafService } from '../src/service.js';

if (process.env.FIREBASE_AUTH_EMULATOR_HOST !== '127.0.0.1:9099' || process.env.FIRESTORE_EMULATOR_HOST !== '127.0.0.1:8080') {
  throw new Error('This seed script is restricted to localhost emulators.');
}
// Test only: this script refuses to run unless both emulators are on localhost. CALAF_TEST_PASSWORD overrides the
// shared fixture password for the fictional members, so tests never need it written into the app.
const memberPassword = process.env.CALAF_TEST_PASSWORD ?? 'CalafDemo!2026';
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
  const password = uid === 'demo-admin' ? 'CalafAdmin!2026' : memberPassword;
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
console.log('Local fixtures ready. All profiles are fictional and exist only in the local emulators.');
console.log('Sign in as hassan@calaf.test (matched with Amina) with the fixture password.');
console.log('Instrumented tests: -Pandroid.testInstrumentationRunnerArguments.calafTestEmail=hassan@calaf.test'
  + ' -Pandroid.testInstrumentationRunnerArguments.calafTestPassword=<the fixture password>');
