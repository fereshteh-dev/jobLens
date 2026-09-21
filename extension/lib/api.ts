import type { JobPostingData } from './jobPosting';

/** Mirrors backend/.../adapter/in/web/dto/*.java exactly - see docs/architecture.md §6. */
export interface VerdictDto {
  kind: 'CONFIDENT' | 'UNCERTAIN' | 'IGNORED';
  probability?: number;
}

export interface SeniorityDto {
  claimed: string;
  actual: string;
  titleVsReality: VerdictDto;
}

export interface RedFlagDto {
  kind: string;
  verdict: VerdictDto;
}

export interface SponsorshipDto {
  visa: string;
  relocation: string;
}

export interface GateDto {
  passed: boolean;
  reasons: string[];
}

export interface TalkingPointsDto {
  coverLetterAngles: string[];
  questionsToAsk: string[];
  whyItFits: string[];
}

export interface AnalyzeResponseDto {
  seniority: SeniorityDto;
  redFlags: RedFlagDto[];
  sponsorship: SponsorshipDto;
  gate: GateDto;
  talkingPoints?: TalkingPointsDto;
}

interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
}

export class AnalyzeApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message);
    this.name = 'AnalyzeApiError';
  }
}

export async function analyzePosting(
  backendUrl: string,
  apiKey: string,
  posting: JobPostingData,
): Promise<AnalyzeResponseDto> {
  const url = `${backendUrl.replace(/\/+$/, '')}/api/v1/analyze`;
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(apiKey ? { 'X-API-Key': apiKey } : {}),
    },
    body: JSON.stringify(posting),
  });

  if (!response.ok) {
    const problem: ProblemDetail = await response.json().catch(() => ({}) as ProblemDetail);
    throw new AnalyzeApiError(
      problem.detail ?? `Request failed with status ${response.status}`,
      response.status,
    );
  }

  return (await response.json()) as AnalyzeResponseDto;
}
