import { Branch } from './branch.model';

export interface Project {
  id: number;
  name: string;
  webUrl: string;
  mostRecentBranchName: String;
  activeBranches: number;
  staleBranches: number;
  mergedBranches: number;
  unmergedBranches: number;
  lastActivityAt: String;
  defaultBranch: String;
}
