import { getSettings, isPrivacyNoticeAcknowledged, acknowledgePrivacyNotice } from '../../lib/storage';
import { analyzePosting, AnalyzeApiError } from '../../lib/api';
import type { ExtractPostingRequest, ExtractPostingResponse } from '../../lib/messages';
import { renderResult } from '../../lib/render';

const privacySection = document.querySelector<HTMLElement>('#privacy-notice');
const noSettingsSection = document.querySelector<HTMLElement>('#no-settings');
const mainSection = document.querySelector<HTMLElement>('#main-view');
const privacyAckButton = document.querySelector<HTMLButtonElement>('#privacy-ack');
const openOptionsButton = document.querySelector<HTMLButtonElement>('#open-options');
const analyzeButton = document.querySelector<HTMLButtonElement>('#analyze-button');
const statusEl = document.querySelector<HTMLElement>('#status');
const resultEl = document.querySelector<HTMLElement>('#result');

async function init(): Promise<void> {
  if (!(await isPrivacyNoticeAcknowledged())) {
    show(privacySection);
    return;
  }
  await showMainOrSettingsPrompt();
}

async function showMainOrSettingsPrompt(): Promise<void> {
  const settings = await getSettings();
  show(settings.backendUrl ? mainSection : noSettingsSection);
}

function show(section: HTMLElement | null): void {
  for (const el of [privacySection, noSettingsSection, mainSection]) {
    if (el) {
      el.hidden = el !== section;
    }
  }
}

privacyAckButton?.addEventListener('click', () => {
  void acknowledgePrivacyNotice().then(showMainOrSettingsPrompt);
});

openOptionsButton?.addEventListener('click', () => {
  void browser.runtime.openOptionsPage();
});

analyzeButton?.addEventListener('click', () => {
  void runAnalysis();
});

async function runAnalysis(): Promise<void> {
  if (!analyzeButton) {
    return;
  }
  if (resultEl) {
    resultEl.innerHTML = '';
  }
  setStatus('Reading posting...', false);
  analyzeButton.disabled = true;

  try {
    const [tab] = await browser.tabs.query({ active: true, currentWindow: true });
    if (!tab?.id) {
      throw new Error('No active tab.');
    }

    const request: ExtractPostingRequest = { type: 'EXTRACT_POSTING' };
    const extracted = await sendMessageToTab(tab.id, request);

    if (!extracted.ok) {
      setStatus(extracted.error, true);
      return;
    }

    setStatus('Analyzing...', false);
    const settings = await getSettings();
    const result = await analyzePosting(settings.backendUrl, settings.apiKey, extracted.posting);

    setStatus('', false);
    if (resultEl) {
      resultEl.innerHTML = renderResult(result);
    }
  } catch (error) {
    setStatus(describeError(error), true);
  } finally {
    analyzeButton.disabled = false;
  }
}

async function sendMessageToTab(
  tabId: number,
  message: ExtractPostingRequest,
): Promise<ExtractPostingResponse> {
  try {
    const response = (await browser.tabs.sendMessage(tabId, message)) as ExtractPostingResponse | undefined;
    if (!response) {
      throw new Error();
    }
    return response;
  } catch {
    throw new Error('Open a supported job posting (currently: LinkedIn) and try again.');
  }
}

function describeError(error: unknown): string {
  if (error instanceof AnalyzeApiError) {
    if (error.status === 401) {
      return 'Backend rejected the API key. Check it in options.';
    }
    if (error.status === 429) {
      return 'Rate limited - try again in a moment.';
    }
    return error.message;
  }
  return error instanceof Error ? error.message : 'Something went wrong.';
}

function setStatus(message: string, isError: boolean): void {
  if (!statusEl) {
    return;
  }
  statusEl.textContent = message;
  statusEl.classList.toggle('error', isError);
}

void init();
