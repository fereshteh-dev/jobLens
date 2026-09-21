/** Shape a SiteAdapter extracts and the backend's AnalyzeRequest expects - see backend's AnalyzeRequest.java. */
export interface JobPostingData {
  url: string;
  title: string;
  company: string;
  location: string;
  descriptionText: string;
}
