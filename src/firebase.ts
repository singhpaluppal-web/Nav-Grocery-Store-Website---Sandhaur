import { FirebaseApp, getApp, getApps, initializeApp } from 'firebase/app';
import {
  Auth,
  GoogleAuthProvider,
  User,
  getAuth,
  getRedirectResult,
  onAuthStateChanged,
  signInWithPopup,
  signInWithRedirect,
  signOut,
} from 'firebase/auth';
import { Database, getDatabase } from 'firebase/database';

export const FIREBASE_DATABASE_URL =
  (import.meta.env.VITE_FIREBASE_DATABASE_URL as string | undefined)?.trim() ||
  'https://nav-grocery-store-default-rtdb.firebaseio.com';

const firebaseConfig = {
  apiKey: (import.meta.env.VITE_FIREBASE_API_KEY as string | undefined)?.trim() || 'AIzaSyDUaToRs_hhQNvI4-q5J611uN_stjUr1-o',
  authDomain:
    (import.meta.env.VITE_FIREBASE_AUTH_DOMAIN as string | undefined)?.trim() ||
    'nav-grocery-store.firebaseapp.com',
  projectId:
    (import.meta.env.VITE_FIREBASE_PROJECT_ID as string | undefined)?.trim() ||
    'nav-grocery-store',
  appId: (import.meta.env.VITE_FIREBASE_APP_ID as string | undefined)?.trim() || '1:272584690336:web:76c20bd0cce7cfe34997c5',
  databaseURL: FIREBASE_DATABASE_URL,
};

export const isFirebaseAuthConfigured = Boolean(
  firebaseConfig.apiKey && firebaseConfig.projectId && firebaseConfig.appId
);

let firebaseApp: FirebaseApp | null = null;
let firebaseAuth: Auth | null = null;
let firebaseDb: Database | null = null;
let googleProvider: GoogleAuthProvider | null = null;

try {
  if (isFirebaseAuthConfigured) {
    firebaseApp = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);
    firebaseAuth = getAuth(firebaseApp);
    googleProvider = new GoogleAuthProvider();
    googleProvider.setCustomParameters({ prompt: 'select_account' });
    firebaseDb = getDatabase(firebaseApp, FIREBASE_DATABASE_URL);
  }
} catch (err) {
  console.warn('[Firebase] Safe initialization fallback active:', err);
  firebaseApp = null;
  firebaseAuth = null;
  firebaseDb = null;
  googleProvider = null;
}

export { firebaseApp, firebaseAuth, firebaseDb, googleProvider };

export interface GoogleSignInResult {
  user: User | null;
  redirected?: boolean;
  error?: string;
}

/**
 * Initiates Firebase GoogleAuthProvider sign-in using signInWithPopup with
 * automatic fallback to signInWithRedirect if popups are blocked or unsupported.
 */
export async function signInWithGoogle(): Promise<GoogleSignInResult> {
  if (!firebaseAuth || !googleProvider) {
    return {
      user: null,
      error:
        'Firebase Google Sign-In environment variables (VITE_FIREBASE_API_KEY, VITE_FIREBASE_AUTH_DOMAIN, VITE_FIREBASE_PROJECT_ID, VITE_FIREBASE_APP_ID) are not configured in this environment yet.',
    };
  }

  try {
    const result = await signInWithPopup(firebaseAuth, googleProvider);
    return { user: result.user };
  } catch (err: unknown) {
    const code =
      typeof err === 'object' && err !== null && 'code' in err
        ? String((err as { code?: unknown }).code || '')
        : '';

    if (
      code === 'auth/popup-blocked' ||
      code === 'auth/operation-not-supported-in-this-environment'
    ) {
      try {
        await signInWithRedirect(firebaseAuth, googleProvider);
        return { user: null, redirected: true };
      } catch (redirectErr) {
        return {
          user: null,
          error:
            redirectErr instanceof Error
              ? redirectErr.message
              : 'Google redirect sign-in failed. Please try again.',
        };
      }
    }

    if (code === 'auth/popup-closed-by-user' || code === 'auth/cancelled-popup-request') {
      return { user: null, error: 'Google sign-in popup was closed before completing.' };
    }

    if (code === 'auth/unauthorized-domain') {
      return {
        user: null,
        error:
          'This domain is not yet authorized in Firebase Authentication -> Settings -> Authorized domains.',
      };
    }

    return {
      user: null,
      error: err instanceof Error ? err.message : 'Unable to sign in with Google right now.',
    };
  }
}

export async function consumeGoogleRedirectResult(): Promise<User | null> {
  if (!firebaseAuth) return null;
  try {
    const result = await getRedirectResult(firebaseAuth);
    return result?.user || null;
  } catch {
    return null;
  }
}

export async function signOutFromGoogle(): Promise<void> {
  if (!firebaseAuth) return;
  try {
    await signOut(firebaseAuth);
  } catch {
    // ignore sign-out error
  }
}

export function subscribeToAuthState(callback: (user: User | null) => void): () => void {
  if (!firebaseAuth) {
    callback(null);
    return () => {};
  }
  return onAuthStateChanged(firebaseAuth, callback);
}
