import { readFileSync } from 'node:fs';
import { before, after, beforeEach, test } from 'node:test';
import assert from 'node:assert/strict';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, collection, getDoc, getDocs, setDoc, updateDoc, deleteDoc, query, where, writeBatch, serverTimestamp, increment, Timestamp } from 'firebase/firestore';

let environment;
const gameId = 'a'.repeat(64);
const otherId = 'b'.repeat(64);
const gamePath = `games/${gameId}`;
const stamp = Timestamp.fromMillis(1000);
const seed = (owner = 'alice', status = 'approved') => ({
  url: 'https://example.com/', normalizedUrl: 'https://example.com/', host: 'example.com', title: 'ทดสอบเกม', description: '', imageUrl: '', tags: ['Puzzle'], orientation: 'any',
  submittedBy: owner, createdAt: stamp, updatedAt: stamp, status, ratingSum: 0, ratingCount: 0, ratingAvg: 0, commentCount: 0,
});
const db = uid => uid ? environment.authenticatedContext(uid).firestore() : environment.unauthenticatedContext().firestore();
before(async () => {
  environment = await initializeTestEnvironment({ projectId: 'demo-vibedeck', firestore: { rules: readFileSync(new URL('../../firestore.rules', import.meta.url), 'utf8') } });
});
after(async () => environment?.cleanup());
beforeEach(async () => {
  await environment.clearFirestore();
  await environment.withSecurityRulesDisabled(async context => {
    const database = context.firestore();
    await setDoc(doc(database, gamePath), seed());
    await setDoc(doc(database, `games/${otherId}`), seed('alice', 'pending'));
    for (const uid of ['alice', 'bob']) await setDoc(doc(database, `users/${uid}`), { displayName: uid, photoUrl: '', createdAt: stamp });
  });
});

test('public reads approved games but cannot read pending games', async () => {
  await assertSucceeds(getDoc(doc(db(), gamePath)));
  await assertFails(getDoc(doc(db(), `games/${otherId}`)));
  await assertSucceeds(getDoc(doc(db('alice'), `games/${otherId}`)));
  await assertSucceeds(getDocs(query(collection(db(), 'games'), where('status', '==', 'approved'))));
  await assertFails(getDocs(collection(db(), 'games')));
});
test('game create requires login, owner match, canonical ID shape and zero aggregates', async () => {
  const fields = { ...seed(), createdAt: serverTimestamp(), updatedAt: serverTimestamp() };
  await assertFails(setDoc(doc(db(), `games/${'c'.repeat(64)}`), fields));
  await assertFails(setDoc(doc(db('bob'), `games/${'c'.repeat(64)}`), fields));
  await assertFails(setDoc(doc(db('alice'), 'games/wrong-id'), fields));
  await assertFails(setDoc(doc(db('alice'), `games/${'c'.repeat(64)}`), { ...fields, ratingSum: 5, ratingCount: 1, ratingAvg: 5 }));
  await assertSucceeds(setDoc(doc(db('alice'), `games/${'c'.repeat(64)}`), fields));
});
test('only owner can edit metadata; URLs, owner and status stay immutable', async () => {
  await assertSucceeds(updateDoc(doc(db('alice'), gamePath), { title: 'ชื่อใหม่', updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(db('bob'), gamePath), { title: 'ขโมยชื่อ', updatedAt: serverTimestamp() }));
  for (const fields of [{ normalizedUrl: 'https://evil.example/' }, { submittedBy: 'bob' }, { status: 'hidden' }]) {
    await assertFails(updateDoc(doc(db('alice'), gamePath), { ...fields, updatedAt: serverTimestamp() }));
  }
});
async function rating(database, previous, value, originalCreatedAt) {
  const batch = writeBatch(database);
  const ref = doc(database, `${gamePath}/ratings/bob`);
  const count = value == null ? 0 : 1;
  if (value == null) batch.delete(ref);
  else batch.set(ref, { value, createdAt: originalCreatedAt || serverTimestamp(), updatedAt: serverTimestamp() });
  batch.update(doc(database, gamePath), { ratingSum: value || 0, ratingCount: count, ratingAvg: value || 0, updatedAt: serverTimestamp() });
  return batch.commit();
}
test('non-owner can atomically add, edit and delete own rating with correct aggregates', async () => {
  const database = db('bob');
  await assertSucceeds(rating(database, null, 5));
  const original = (await getDoc(doc(database, `${gamePath}/ratings/bob`))).data().createdAt;
  await assertSucceeds(rating(database, 5, 2, original));
  assert.equal((await getDoc(doc(database, gamePath))).data().ratingSum, 2);
  await assertSucceeds(rating(database, 2, null));
  const final = (await getDoc(doc(database, gamePath))).data();
  assert.equal(final.ratingCount, 0); assert.equal(final.ratingAvg, 0);
});
test('rating without parent update and direct aggregate spoof are denied', async () => {
  await assertFails(setDoc(doc(db('bob'), `${gamePath}/ratings/bob`), { value: 5, createdAt: serverTimestamp(), updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(db('bob'), gamePath), { ratingSum: 500, ratingCount: 100, ratingAvg: 5, updatedAt: serverTimestamp() }));
  await assertFails(updateDoc(doc(db('alice'), gamePath), { title: 'new', ratingSum: 5, ratingCount: 1, ratingAvg: 5, updatedAt: serverTimestamp() }));
});
test('cross-user, fractional and out-of-range ratings are denied', async () => {
  for (const [uid, value] of [['alice', 5], ['bob', 6], ['bob', 1.5], ['bob', 0]]) {
    const database = db('bob'); const batch = writeBatch(database);
    batch.set(doc(database, `${gamePath}/ratings/${uid}`), { value, createdAt: serverTimestamp(), updatedAt: serverTimestamp() });
    batch.update(doc(database, gamePath), { ratingSum: value, ratingCount: 1, ratingAvg: value, updatedAt: serverTimestamp() });
    await assertFails(batch.commit());
  }
});
test('wrong average and wrong delta are denied even with a valid rating document', async () => {
  const database = db('bob');
  for (const [sum, average] of [[5, 4], [4, 4]]) {
    const batch = writeBatch(database);
    batch.set(doc(database, `${gamePath}/ratings/bob`), { value: 5, createdAt: serverTimestamp(), updatedAt: serverTimestamp() });
    batch.update(doc(database, gamePath), { ratingSum: sum, ratingCount: 1, ratingAvg: average, updatedAt: serverTimestamp() });
    await assertFails(batch.commit());
  }
});
function commentBatch(database, id, uid = 'bob', text = 'สนุกมาก') {
  const batch = writeBatch(database);
  batch.set(doc(database, `${gamePath}/comments/${id}`), { uid, authorName: uid, authorPhotoUrl: '', text, createdAt: serverTimestamp() });
  batch.update(doc(database, gamePath), { commentCount: increment(1), commentMutationId: id, updatedAt: serverTimestamp() });
  return batch;
}
test('comments require atomic counter update and correct author', async () => {
  const database = db('bob');
  await assertFails(setDoc(doc(database, `${gamePath}/comments/bare`), { uid: 'bob', authorName: 'bob', authorPhotoUrl: '', text: 'hi', createdAt: serverTimestamp() }));
  await assertFails(commentBatch(database, 'spoof', 'alice').commit());
  await assertFails(commentBatch(database, 'long', 'bob', 'x'.repeat(501)).commit());
  await assertSucceeds(commentBatch(database, 'first').commit());
  assert.equal((await getDoc(doc(database, gamePath))).data().commentCount, 1);
});
test('comments cannot be edited; only author can delete with matching decrement', async () => {
  await commentBatch(db('bob'), 'first').commit();
  await assertFails(updateDoc(doc(db('bob'), `${gamePath}/comments/first`), { text: 'edited' }));
  await assertFails(deleteDoc(doc(db('bob'), `${gamePath}/comments/first`)));
  for (const uid of ['alice', 'bob']) {
    const database = db(uid); const batch = writeBatch(database);
    batch.delete(doc(database, `${gamePath}/comments/first`));
    batch.update(doc(database, gamePath), { commentCount: increment(-1), commentMutationId: 'first', updatedAt: serverTimestamp() });
    if (uid === 'alice') await assertFails(batch.commit()); else await assertSucceeds(batch.commit());
  }
});
test('counter spoof, dangling mutation pointer and replayed increments fail', async () => {
  await assertFails(updateDoc(doc(db('bob'), gamePath), { commentCount: 99, commentMutationId: 'missing', updatedAt: serverTimestamp() }));
  await commentBatch(db('bob'), 'first').commit();
  await assertFails(updateDoc(doc(db('bob'), gamePath), { commentCount: 2, commentMutationId: 'first', updatedAt: serverTimestamp() }));
  await assertFails(commentBatch(db('bob'), 'first').commit());
});
test('reports are create-only and cannot be read even by the reporter', async () => {
  const database = db('bob'); const ref = doc(collection(database, 'reports'));
  await assertSucceeds(setDoc(ref, { targetType: 'game', gameId, reporterUid: 'bob', reason: 'เล่นไม่ได้', createdAt: serverTimestamp() }));
  await assertFails(getDoc(ref)); await assertFails(updateDoc(ref, { reason: 'changed' })); await assertFails(deleteDoc(ref));
});
test('profiles cannot be written by another user or acquire extra fields', async () => {
  await assertFails(updateDoc(doc(db('bob'), 'users/alice'), { displayName: 'spoof' }));
  await assertFails(updateDoc(doc(db('alice'), 'users/alice'), { admin: true }));
  await assertSucceeds(updateDoc(doc(db('alice'), 'users/alice'), { displayName: 'Alice' }));
});
