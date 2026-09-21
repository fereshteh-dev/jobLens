import { describe, expect, it } from 'vitest';
import { renderResult } from './render';
import type { AnalyzeResponseDto } from './api';

const BASE_RESULT: AnalyzeResponseDto = {
  seniority: {
    claimed: 'SENIOR',
    actual: 'MID',
    titleVsReality: { kind: 'CONFIDENT', probability: 0.87 },
  },
  redFlags: [
    { kind: 'HIDDEN_OVERTIME', verdict: { kind: 'UNCERTAIN', probability: 0.55 } },
    { kind: 'VAGUE_SCOPE', verdict: { kind: 'IGNORED' } },
  ],
  sponsorship: { visa: 'UNLIKELY', relocation: 'UNKNOWN' },
  gate: { passed: false, reasons: ['Actual seniority MID does not match target SENIOR'] },
};

describe('renderResult', () => {
  it('shows claimed vs actual seniority and the title-match verdict with its probability', () => {
    const html = renderResult(BASE_RESULT);
    expect(html).toContain('senior');
    expect(html).toContain('mid');
    expect(html).toContain('CONFIDENT (87%)');
  });

  it('renders a badge for every red flag, including ones with no probability', () => {
    const html = renderResult(BASE_RESULT);
    expect(html).toContain('hidden overtime');
    expect(html).toContain('UNCERTAIN (55%)');
    expect(html).toContain('vague scope');
    expect(html).toContain('>IGNORED<');
  });

  it('shows gate reasons when the gate failed', () => {
    const html = renderResult(BASE_RESULT);
    expect(html).toContain('Not a match');
    expect(html).toContain('Actual seniority MID does not match target SENIOR');
  });

  it('omits the talking points card when the gate failed (no talkingPoints field)', () => {
    const html = renderResult(BASE_RESULT);
    expect(html).not.toContain('Talking points');
  });

  it('renders talking points when present', () => {
    const passing: AnalyzeResponseDto = {
      ...BASE_RESULT,
      gate: { passed: true, reasons: [] },
      talkingPoints: {
        coverLetterAngles: ['Angle one'],
        questionsToAsk: ['Question one'],
        whyItFits: ['Fit reason one'],
      },
    };

    const html = renderResult(passing);

    expect(html).toContain('Talking points');
    expect(html).toContain('Angle one');
    expect(html).toContain('Question one');
    expect(html).toContain('Fit reason one');
    expect(html).toContain('Passed');
  });

  it('escapes HTML-unsafe characters in backend-provided text', () => {
    const withUnsafeReason: AnalyzeResponseDto = {
      ...BASE_RESULT,
      gate: { passed: false, reasons: ['<script>alert(1)</script>'] },
    };

    const html = renderResult(withUnsafeReason);

    expect(html).not.toContain('<script>');
    expect(html).toContain('&lt;script&gt;');
  });
});
