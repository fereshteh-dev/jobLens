import { getSettings, saveSettings } from '../../lib/storage';

async function init(): Promise<void> {
  const form = document.querySelector<HTMLFormElement>('#settings-form');
  const backendUrlInput = document.querySelector<HTMLInputElement>('#backend-url');
  const apiKeyInput = document.querySelector<HTMLInputElement>('#api-key');
  const savedNotice = document.querySelector<HTMLElement>('#saved-notice');
  if (!form || !backendUrlInput || !apiKeyInput || !savedNotice) {
    return;
  }

  const settings = await getSettings();
  backendUrlInput.value = settings.backendUrl;
  apiKeyInput.value = settings.apiKey;

  form.addEventListener('submit', (event) => {
    event.preventDefault();
    void saveSettings({ backendUrl: backendUrlInput.value.trim(), apiKey: apiKeyInput.value.trim() }).then(() => {
      savedNotice.hidden = false;
      setTimeout(() => {
        savedNotice.hidden = true;
      }, 2000);
    });
  });
}

void init();
