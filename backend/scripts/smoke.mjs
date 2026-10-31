import assert from 'node:assert/strict';
const auth = await fetch('http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=local-test', {
  method: 'POST', headers: { 'content-type': 'application/json' },
  body: JSON.stringify({ email: 'hassan@calaf.test', password: 'CalafDemo!2026', returnSecureToken: true }),
}).then(response => response.json());
assert.ok(auth.idToken, 'Local sign-in failed');
const result = await fetch('http://127.0.0.1:5001/demo-calaf/europe-west2/calaf', {
  method: 'POST', headers: { 'content-type': 'application/json', authorization: `Bearer ${auth.idToken}` },
  body: JSON.stringify({ data: { action: 'discover' } }),
}).then(response => response.json());
assert.ok(Array.isArray(result.result?.profiles), `Callable failed: ${result.error?.status}`);
assert.ok(result.result.profiles.every(profile => !('birthDate' in profile) && !('email' in profile)));
console.log(`PASS: local Auth -> callable discovery. ${result.result.profiles.length} eligible fictional profiles; no DOB/email in projection.`);
