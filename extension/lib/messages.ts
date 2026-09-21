import type { JobPostingData } from './jobPosting';

/** Typed contract for side-panel <-> content-script messaging (chrome.tabs.sendMessage). */
export interface ExtractPostingRequest {
  type: 'EXTRACT_POSTING';
}

export type ExtractPostingResponse =
  | { ok: true; posting: JobPostingData }
  | { ok: false; error: string };

export function isExtractPostingRequest(message: unknown): message is ExtractPostingRequest {
  return (
    typeof message === 'object' &&
    message !== null &&
    (message as { type?: unknown }).type === 'EXTRACT_POSTING'
  );
}
