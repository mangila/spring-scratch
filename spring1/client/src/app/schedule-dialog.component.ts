import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { Generation } from '../api/model/generation';

@Component({
  selector: 'app-schedule-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
  ],
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <h2 mat-dialog-title class="dialog-title">
          <mat-icon class="header-icon">sync</mat-icon>
          Schedule Pokémon Ingestion
        </h2>
        <button mat-icon-button mat-dialog-close aria-label="Close dialog">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-dialog-content class="dialog-body">
        <p class="desc">
          Enqueue a background job via the PokeAPI connector to fetch and populate Pokémon records
          into the database.
        </p>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Select Generation</mat-label>
          <mat-select [(ngModel)]="selectedGeneration">
            <mat-option value="generation-i">Generation I (Kanto, #1 - #151)</mat-option>
          </mat-select>
        </mat-form-field>
      </mat-dialog-content>

      <mat-dialog-actions align="end" class="dialog-actions">
        <button mat-stroked-button mat-dialog-close>Cancel</button>
        <button
          mat-flat-button
          color="primary"
          [disabled]="!selectedGeneration"
          (click)="onConfirm()"
        >
          <mat-icon>play_arrow</mat-icon>
          Schedule Job
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        padding: 8px 16px;
        min-width: 320px;
        max-width: 440px;
      }
      .dialog-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding-bottom: 8px;
      }
      .dialog-title {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0;
        font-size: 1.25rem;
        font-weight: 700;
        color: #1e293b;
      }
      .header-icon {
        color: #3b82f6;
      }
      .desc {
        color: #64748b;
        font-size: 0.9rem;
        line-height: 1.4;
        margin-bottom: 16px;
      }
      .full-width {
        width: 100%;
      }
      .dialog-actions {
        padding-top: 16px;
        gap: 8px;
      }
    `,
  ],
})
export class ScheduleDialogComponent {
  readonly dialogRef = inject(MatDialogRef<ScheduleDialogComponent>);
  selectedGeneration: Generation = 'generation-i';

  onConfirm(): void {
    this.dialogRef.close(this.selectedGeneration);
  }
}
