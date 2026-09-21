/**
 * `browser` is WXT's ambient global (from wxt/browser), not the raw `chrome` API. Uses
 * `.local` (not `.sync`) - the API key is a secret-like value and shouldn't sync across the
 * user's devices/Google account.
 */
export interface Settings {
  backendUrl: string;
  apiKey: string;
}

const STORAGE_KEYS = {
  backendUrl: 'backendUrl',
  apiKey: 'apiKey',
  privacyAcknowledged: 'privacyAcknowledged',
} as const;

export async function getSettings(): Promise<Settings> {
  const stored = await browser.storage.local.get([STORAGE_KEYS.backendUrl, STORAGE_KEYS.apiKey]);
  return {
    backendUrl: (stored[STORAGE_KEYS.backendUrl] as string | undefined) ?? '',
    apiKey: (stored[STORAGE_KEYS.apiKey] as string | undefined) ?? '',
  };
}

export async function saveSettings(settings: Settings): Promise<void> {
  await browser.storage.local.set({
    [STORAGE_KEYS.backendUrl]: settings.backendUrl,
    [STORAGE_KEYS.apiKey]: settings.apiKey,
  });
}

export async function isPrivacyNoticeAcknowledged(): Promise<boolean> {
  const stored = await browser.storage.local.get(STORAGE_KEYS.privacyAcknowledged);
  return stored[STORAGE_KEYS.privacyAcknowledged] === true;
}

export async function acknowledgePrivacyNotice(): Promise<void> {
  await browser.storage.local.set({ [STORAGE_KEYS.privacyAcknowledged]: true });
}
