import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { Observable, map, of, shareReplay } from 'rxjs';
import { PokemonDto } from '../api/model/pokemonDto';
import { PokemonService } from '../api/api/pokemon.service';

@Component({
  selector: 'app-pokemon-detail-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule, MatChipsModule],
  template: `
    <div class="dialog-container">
      <div class="dialog-header">
        <div class="header-info">
          <span class="dex-badge">#{{ padDexId(data.pokemon.pokedexId) }}</span>
          <h2 mat-dialog-title class="pokemon-name">{{ data.pokemon.name }}</h2>
        </div>
        <button mat-icon-button mat-dialog-close class="close-btn" aria-label="Close dialog">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <mat-dialog-content class="dialog-body">
        <div class="image-wrapper">
          <img
            [src]="getPokemonImageUrl(data.pokemon)"
            [alt]="data.pokemon.name"
            class="pokemon-large-img"
          />
        </div>

        <div class="details-grid">
          <div class="detail-card">
            <span class="detail-label">National Pokédex ID</span>
            <span class="detail-val">#{{ data.pokemon.pokedexId }}</span>
          </div>
          <div class="detail-card">
            <span class="detail-label">Generation</span>
            <span class="detail-val">{{ formatGeneration(data.pokemon.generation) }}</span>
          </div>
          <div class="detail-card">
            <span class="detail-label">Species Name</span>
            <span class="detail-val capitalized">{{ data.pokemon.name }}</span>
          </div>
          <div class="detail-card">
            <span class="detail-label">Region</span>
            <span class="detail-val">Kanto (Gen I)</span>
          </div>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end" class="dialog-actions">
        <button mat-stroked-button mat-dialog-close>Close</button>
        <button mat-flat-button color="warn" (click)="onDelete()">
          <mat-icon>delete</mat-icon>
          Delete Pokémon
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [
    `
      .dialog-container {
        padding: 8px 16px;
        min-width: 340px;
        max-width: 480px;
      }
      .dialog-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid rgba(0, 0, 0, 0.08);
        padding-bottom: 8px;
      }
      .header-info {
        display: flex;
        align-items: center;
        gap: 12px;
      }
      .dex-badge {
        background: linear-gradient(135deg, #e3350d, #ff5722);
        color: #fff;
        font-weight: 700;
        font-size: 0.85rem;
        padding: 4px 10px;
        border-radius: 20px;
        letter-spacing: 0.5px;
      }
      .pokemon-name {
        margin: 0;
        font-size: 1.5rem;
        font-weight: 700;
        text-transform: capitalize;
        letter-spacing: -0.5px;
      }
      .image-wrapper {
        display: flex;
        justify-content: center;
        align-items: center;
        margin: 20px 0;
        background: radial-gradient(circle, rgba(238, 82, 83, 0.15) 0%, rgba(255, 255, 255, 0) 70%);
        border-radius: 16px;
        padding: 16px;
      }
      .pokemon-large-img {
        width: 180px;
        height: 180px;
        object-fit: contain;
        filter: drop-shadow(0 10px 15px rgba(0, 0, 0, 0.15));
        transition: transform 0.3s ease;
      }
      .pokemon-large-img:hover {
        transform: scale(1.08);
      }
      .details-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 12px;
        margin-top: 12px;
      }
      .detail-card {
        background: #f7f9fc;
        border: 1px solid #e2e8f0;
        border-radius: 12px;
        padding: 12px 14px;
        display: flex;
        flex-direction: column;
        gap: 4px;
      }
      .detail-label {
        font-size: 0.75rem;
        color: #64748b;
        text-transform: uppercase;
        font-weight: 600;
        letter-spacing: 0.5px;
      }
      .detail-val {
        font-size: 1rem;
        font-weight: 600;
        color: #1e293b;
      }
      .capitalized {
        text-transform: capitalize;
      }
      .dialog-actions {
        padding-top: 16px;
        gap: 8px;
      }
    `,
  ],
})
export class PokemonDetailDialogComponent {
  readonly dialogRef = inject(MatDialogRef<PokemonDetailDialogComponent>);
  readonly data: { pokemon: PokemonDto } = inject(MAT_DIALOG_DATA);
  private readonly pokemonService = inject(PokemonService, { optional: false });
  private readonly sanitizer = inject(DomSanitizer);

  padDexId(id: number): string {
    return String(id).padStart(3, '0');
  }

  formatGeneration(gen?: string): string {
    if (!gen) return 'Unknown';
    if (gen === 'generation-i') return 'Generation I';
    return gen.replace('-', ' ').toUpperCase();
  }

  getPokemonImageUrl(pokemon: PokemonDto): string {
    return this.pokemonService.configuration.basePath + pokemon.imageUrl;
  }

  onDelete(): void {
    this.dialogRef.close({ action: 'delete', pokemon: this.data.pokemon });
  }
}
