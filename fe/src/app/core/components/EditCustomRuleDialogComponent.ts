import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  MatDialogRef,
  MatDialogModule,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CustomRuleService } from '../services/customRule.service';
import {
  CustomRule,
  RuleCategory,
  RuleSeverity,
} from '../models/customRule.model';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ErrorHandlingService } from '../services/errorHandling.service';

@Component({
  selector: 'app-edit-custom-rule-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
  ],
  template: `
    <h2 mat-dialog-title>Edit Custom Rule</h2>
    <form [formGroup]="ruleForm" (ngSubmit)="onSubmit()">
      <mat-dialog-content>
        <div class="form-container">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Description</mat-label>
            <textarea
              matInput
              formControlName="description"
              placeholder="Short description about why this rule is important and consequences of not following it"
              rows="4"
            ></textarea>
            <mat-error
              *ngIf="ruleForm.get('description')?.errors?.['required']"
            >
              Description is required
            </mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Remark (Optional)</mat-label>
            <textarea
              matInput
              formControlName="remark"
              placeholder="Enter example that violates this rule"
              rows="4"
            ></textarea>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Severity</mat-label>
            <mat-select formControlName="severity">
              <mat-option
                *ngFor="let severity of severityLevels"
                [value]="severity"
              >
                {{ severity }}
              </mat-option>
            </mat-select>
            <mat-error *ngIf="ruleForm.get('severity')?.errors?.['required']">
              Severity is required
            </mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Category</mat-label>
            <mat-select formControlName="category">
              <mat-option
                *ngFor="let category of categoryOptions"
                [value]="category"
              >
                {{
                  category === 'FE'
                    ? 'Frontend (FE)'
                    : category === 'BE'
                    ? 'Backend (BE)'
                    : 'Service (SVC)'
                }}
              </mat-option>
            </mat-select>
            <mat-error *ngIf="ruleForm.get('category')?.errors?.['required']">
              Category is required
            </mat-error>
          </mat-form-field>

          <div class="enabled-toggle">
            <mat-slide-toggle formControlName="enabled" color="primary">
              Enable Rule
            </mat-slide-toggle>
          </div>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" (click)="onCancel()">Cancel</button>
        <button
          mat-flat-button
          color="primary"
          type="submit"
          [disabled]="ruleForm.invalid || isSubmitting"
        >
          <span *ngIf="isSubmitting">Updating...</span>
          <span *ngIf="!isSubmitting">Update Rule</span>
        </button>
      </mat-dialog-actions>

      <div *ngIf="error" class="error-message">
        {{ error }}
      </div>
    </form>
  `,
  styles: [
    `
      .form-container {
        display: flex;
        flex-direction: column;
        gap: 1rem;
        padding: 1rem 0;
      }

      .full-width {
        width: 100%;
      }

      mat-dialog-content {
        min-width: 400px;
      }

      textarea {
        min-height: 100px;
      }

      .enabled-toggle {
        margin-top: 1rem;
      }

      .error-message {
        color: #b71c1c;
        margin-top: 1rem;
        padding: 0.5rem;
        text-align: center;
        background-color: #ffebee;
        border-radius: 4px;
      }
    `,
  ],
})
export class EditCustomRuleDialogComponent {
  ruleForm: FormGroup;
  severityLevels = Object.values(RuleSeverity);
  categoryOptions = Object.values(RuleCategory);
  isSubmitting = false;
  error = '';
  rule: CustomRule;

  constructor(
    private dialogRef: MatDialogRef<EditCustomRuleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CustomRule,
    private fb: FormBuilder,
    private customRuleService: CustomRuleService,
    private errorHandler: ErrorHandlingService
  ) {
    this.rule = data;

    // Initialize form with existing rule data
    this.ruleForm = this.fb.group({
      id: [this.rule.id],
      description: [this.rule.description, [Validators.required]],
      severity: [this.rule.severity, [Validators.required]],
      category: [this.rule.category, [Validators.required]],
      enabled: [this.rule.enabled],
      parentGroupId: [
        { value: this.rule.parentGroupId, disabled: true },
        [Validators.required],
      ],
      remark: [this.rule.remark || ''],
    });
  }

  onSubmit(): void {
    if (this.ruleForm.invalid) {
      return;
    }

    this.isSubmitting = true;
    this.error = '';

    const updatedRule = {
      ...this.ruleForm.getRawValue(), // Use getRawValue to include disabled fields
    };

    this.customRuleService.updateRule(this.rule.id, updatedRule).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.error = this.errorHandler.getErrorMessage(
          err,
          'Failed to update rule'
        );
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
