export interface CustomRule {
  id: number;
  description: string;
  severity: RuleSeverity;
  createdAt: string;
  updatedAt: string;
  enabled: boolean;
  parentGroupId: number;
  remark?: string;
  category: RuleCategory;
}

export enum RuleSeverity {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
}

export enum RuleCategory {
  FE = 'FE',
  BE = 'BE',
  SVC = 'SVC',
}
