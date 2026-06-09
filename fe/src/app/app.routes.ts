import { Routes } from '@angular/router';
import { ProjectsComponent } from './features/projects/projects.component';
import { BranchesComponent } from './features/branches/branches.component';
import { GroupsComponent } from './features/groups/groups.component';
import { SubgroupsComponent } from './features/subgroups/subgroups.component';
import { NotFoundComponent } from './core/components/NotFoundComponent';
import { CustomRulesComponent } from './features/customRules/customRules.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'groups',
    pathMatch: 'full',
  },
  {
    path: 'groups',
    children: [
      {
        path: '',
        component: GroupsComponent,
      },
      {
        path: ':groupId/subgroups',
        component: SubgroupsComponent,
      },
      {
        path: ':groupId/subgroups/:subgroupId/projects',
        component: ProjectsComponent,
      },
      {
        path: ':groupId/subgroups/:subgroupId/projects/:projectId/branches',
        component: BranchesComponent,
      },
    ],
  },
  {
    path: 'codeReviewRules',
    component: CustomRulesComponent,
  },
  {
    path: '**',
    component: NotFoundComponent,
  },
];
