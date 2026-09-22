import { JSDOM } from 'jsdom';
import { describe, expect, it } from 'vitest';
import { linkedInAdapter } from './LinkedInAdapter';

describe('linkedInAdapter.matches', () => {
  it('matches a LinkedIn jobs URL', () => {
    expect(linkedInAdapter.matches('https://www.linkedin.com/jobs/view/1234567')).toBe(true);
  });

  it('does not match a non-jobs LinkedIn URL', () => {
    expect(linkedInAdapter.matches('https://www.linkedin.com/in/someone')).toBe(false);
  });

  it('does not match a different site', () => {
    expect(linkedInAdapter.matches('https://www.indeed.com/jobs/view/1234567')).toBe(false);
  });
});

describe('linkedInAdapter.extract', () => {
  function docWithHtml(bodyHtml: string, url = 'https://www.linkedin.com/jobs/view/1234567'): Document {
    return new JSDOM(`<html><body>${bodyHtml}</body></html>`, { url }).window.document;
  }

  it('extracts title, company, location and description when all are present', () => {
    const doc = docWithHtml(`
      <h1 class="job-details-jobs-unified-top-card__job-title">Senior Backend Engineer</h1>
      <a class="job-details-jobs-unified-top-card__company-name">Acme Corp</a>
      <div class="job-details-jobs-unified-top-card__primary-description-container">Remote</div>
      <div id="job-details">Own our payments platform end to end.</div>
    `);

    const posting = linkedInAdapter.extract(doc);

    expect(posting).toEqual({
      url: 'https://www.linkedin.com/jobs/view/1234567',
      title: 'Senior Backend Engineer',
      company: 'Acme Corp',
      location: 'Remote',
      descriptionText: 'Own our payments platform end to end.',
    });
  });

  it('extracts from the SDUI job-details pane used on search-results pages', () => {
    const doc = docWithHtml(
      `
      <a href="https://www.linkedin.com/jobs/view/999/">Some other job in the results list</a>
      <div data-sdui-screen="com.linkedin.sdui.flagshipnav.jobs.SemanticJobDetails">
        <a href="https://www.linkedin.com/company/cgi/life/"><figure><img alt=""></figure><p>CGI</p></a>
        <p><a href="https://www.linkedin.com/jobs/view/4440315405/?trackingId=x">Senior Java Developers</a></p>
        <p><span>United Kingdom</span><span> </span>·<span> </span><span>Reposted 5 days ago</span></p>
        <div id="JobDetails_AboutTheJob_4440315405">
          <span data-testid="expandable-text-box">Position Description<br><br>At CGI, we build things.<ul><li> Build Spring Boot services</li><li> Ship them</li></ul><button data-testid="expandable-text-button"><span>… more</span></button></span>
        </div>
        <div id="JobDetails_AboutTheCompany_4440315405">
          <span data-testid="expandable-text-box">Founded in 1976.</span>
        </div>
      </div>
    `,
      'https://www.linkedin.com/jobs/search-results/?currentJobId=4440315405',
    );

    const posting = linkedInAdapter.extract(doc);

    expect(posting?.title).toBe('Senior Java Developers');
    expect(posting?.company).toBe('CGI');
    expect(posting?.location).toBe('United Kingdom');
    expect(posting?.descriptionText).toBe(
      'Position Description\n\nAt CGI, we build things.\n Build Spring Boot services\n Ship them',
    );
  });

  it('returns null when the title is missing', () => {
    const doc = docWithHtml(`<div id="job-details">Some description.</div>`);
    expect(linkedInAdapter.extract(doc)).toBeNull();
  });

  it('returns null when the description is missing', () => {
    const doc = docWithHtml(`<h1>Senior Backend Engineer</h1>`);
    expect(linkedInAdapter.extract(doc)).toBeNull();
  });

  it('falls back to empty strings for company/location when only those are missing', () => {
    const doc = docWithHtml(`
      <h1>Senior Backend Engineer</h1>
      <div id="job-details">Own our payments platform end to end.</div>
    `);

    const posting = linkedInAdapter.extract(doc);

    expect(posting?.company).toBe('');
    expect(posting?.location).toBe('');
  });
});
