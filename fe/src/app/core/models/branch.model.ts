import { Commit } from './commit.model';

export interface Branch {
  id: number;
  name: string;
  webUrl: string;
  projectName: String;
  active: boolean;
  commit: Commit;
  merged: boolean;
  mergedBy: string;
  detailedSummary: string;
  needsUpdate: boolean;

  comparingAgainst: string | null;
  compareLoading?: boolean;
}

export interface BranchCompareResponse {
  isMerged: boolean;
  needsUpdate: boolean;
  detailedSummary?: string;
}
