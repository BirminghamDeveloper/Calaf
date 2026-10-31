import { initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { getAuth } from 'firebase-admin/auth';
import { onCall, HttpsError } from 'firebase-functions/v2/https';
import { CalafService } from './service.js';
import { DomainError } from './domain.js';

initializeApp();
const service = new CalafService(getFirestore(), getAuth());
export const calaf = onCall({ region: 'europe-west2', maxInstances: 10,
  enforceAppCheck: process.env.FUNCTIONS_EMULATOR !== 'true', timeoutSeconds: 60 }, async request => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Please log in.');
  if (!request.auth.token.email_verified) throw new HttpsError('permission-denied', 'Verify your email first.');
  try {
    const account = await getAuth().getUser(request.auth.uid);
    if (account.disabled || request.auth.token.auth_time < Date.parse(account.tokensValidAfterTime) / 1000) {
      throw new DomainError('unauthenticated', 'Please log in again.');
    }
    return await service.execute(request.auth.uid, request.data ?? {}, request.auth.token);
  }
  catch (error) {
    if (error instanceof DomainError) throw new HttpsError(error.code, error.message);
    // Do not echo exception details, tokens, messages or personal information.
    throw new HttpsError('internal', 'The request could not be completed. Please retry.');
  }
});
