import { findAdapterFor } from '../lib/siteAdapters/registry';
import { isExtractPostingRequest, type ExtractPostingResponse } from '../lib/messages';

/**
 * Idle until asked. Does nothing on load, on navigation, or on a timer - only extracts the
 * current page's posting when the side panel explicitly asks, which only happens when the
 * user clicks "Analyze this posting". No crawling, no bulk scraping, no auto-anything.
 */
export default defineContentScript({
  matches: ['*://*.linkedin.com/*'],
  main() {
    browser.runtime.onMessage.addListener((message, _sender, sendResponse) => {
      if (!isExtractPostingRequest(message)) {
        return undefined;
      }

      const adapter = findAdapterFor(location.href);
      if (!adapter) {
        const response: ExtractPostingResponse = {
          ok: false,
          error: 'This page is not a supported job posting.',
        };
        sendResponse(response);
        return undefined;
      }

      const posting = adapter.extract(document);
      const response: ExtractPostingResponse = posting
        ? { ok: true, posting }
        : { ok: false, error: 'Could not find a job posting on this page.' };
      sendResponse(response);
      return undefined;
    });
  },
});
