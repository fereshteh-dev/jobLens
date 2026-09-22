import type { JobPostingData } from '../jobPosting';
import type { SiteAdapter } from './SiteAdapter';

/**
 * LinkedIn currently serves two job-detail layouts:
 *
 * - SDUI (search results / collections / recommended): class names are build-hashed and change
 *   per deploy, so we anchor on the stable hooks instead - the `data-sdui-screen` pane, the
 *   `JobDetails_AboutTheJob_*` id, `data-testid` and href patterns. Verified against a live
 *   `/jobs/search-results/` page.
 * - Legacy (`/jobs/view/<id>` with `.jobs-unified-top-card__*` classes): kept as a fallback.
 *
 * If extraction stops working, this is the one file to update; the SiteAdapter interface exists
 * specifically so that fix never touches the rest of the extension.
 */
export const linkedInAdapter: SiteAdapter = {
  name: 'linkedin',

  matches(url) {
    return /(^|\.)linkedin\.com$/.test(new URL(url).hostname) && /\/jobs\//.test(url);
  },

  extract(doc) {
    // The href-based selectors are only safe inside the details pane; on a full document they
    // would also match links in the results list.
    const pane = doc.querySelector('[data-sdui-screen$="SemanticJobDetails"]');

    const title = firstText(pane, ['a[href*="/jobs/view/"]']) ?? firstText(doc, [
      'h1.job-details-jobs-unified-top-card__job-title',
      'h1.t-24',
      '.jobs-unified-top-card__job-title',
      'h1',
    ]);
    const company = firstText(pane, ['a[href*="/company/"]']) ?? firstText(doc, [
      '.job-details-jobs-unified-top-card__company-name a',
      '.job-details-jobs-unified-top-card__company-name',
      '.jobs-unified-top-card__company-name',
    ]);
    const location = sduiLocation(pane) ?? firstText(doc, [
      '.job-details-jobs-unified-top-card__primary-description-container',
      '.jobs-unified-top-card__bullet',
      '.jobs-unified-top-card__subtitle-primary-grouping',
    ]);
    const descriptionText = firstText(pane, [
      '[id^="JobDetails_AboutTheJob_"] [data-testid="expandable-text-box"]',
    ]) ?? firstText(doc, [
      '#job-details',
      '.jobs-description__content',
      '.jobs-box__html-content',
    ]);

    if (!title || !descriptionText) {
      return null;
    }

    const posting: JobPostingData = {
      url: doc.location.href,
      title,
      company: company ?? '',
      location: location ?? '',
      descriptionText,
    };
    return posting;
  },
};

/** SDUI meta line is `<p><span>United Kingdom</span> · <span>Reposted 5 days ago</span> · ...</p>`. */
function sduiLocation(pane: ParentNode | null): string | null {
  if (!pane) {
    return null;
  }
  for (const p of pane.querySelectorAll('p')) {
    if (p.textContent?.includes('·')) {
      const first = p.querySelector('span')?.textContent?.trim();
      if (first) {
        return first;
      }
    }
  }
  return null;
}

function firstText(root: ParentNode | null, selectors: string[]): string | null {
  if (!root) {
    return null;
  }
  for (const selector of selectors) {
    const element = root.querySelector(selector);
    const text = element ? readableText(element) : null;
    if (text) {
      return text;
    }
  }
  return null;
}

/**
 * textContent, minus the "… more" toggle button LinkedIn embeds in collapsed text, and with
 * <br>/<li> turned into newlines so words on adjacent lines don't fuse ("DescriptionAt CGI").
 */
function readableText(element: Element): string | null {
  const copy = element.cloneNode(true) as Element;
  copy.querySelectorAll('[data-testid="expandable-text-button"]').forEach((el) => el.remove());
  copy.querySelectorAll('br').forEach((br) => br.replaceWith('\n'));
  copy.querySelectorAll('li').forEach((li) => li.prepend('\n'));
  const text = copy.textContent?.replace(/\n{3,}/g, '\n\n').trim();
  return text ? text : null;
}
