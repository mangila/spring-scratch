import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { Observable, map, of, shareReplay } from 'rxjs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDividerModule } from '@angular/material/divider';
import { PokemonService } from '../api/api/pokemon.service';
import { PokemonDto } from '../api/model/pokemonDto';
import { Generation } from '../api/model/generation';
import { PokemonDetailDialogComponent } from './pokemon-detail-dialog.component';
import { ScheduleDialogComponent } from './schedule-dialog.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatInputModule,
    MatFormFieldModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatDialogModule,
    MatChipsModule,
    MatTableModule,
    MatTooltipModule,
    MatButtonToggleModule,
    MatDividerModule,
  ],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private readonly pokemonService = inject(PokemonService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly sanitizer = inject(DomSanitizer);

  readonly title = signal('PokéDex Client');
  readonly loading = signal(false);
  readonly pokemonList = signal<PokemonDto[]>([]);
  readonly totalElements = signal(0);
  readonly pageSize = signal(20);
  readonly pageIndex = signal(0);
  readonly viewMode = signal<'grid' | 'table'>('grid');
  readonly searchQuery = signal('');
  readonly isSearchActive = signal(false);
  readonly displayedColumns = ['pokedexId', 'sprite', 'name', 'generation', 'actions'];

  ngOnInit(): void {
    this.loadPokemon();
  }

  loadPokemon(): void {
    this.loading.set(true);
    this.isSearchActive.set(false);
    this.pokemonService.findAllPokemon(this.pageIndex(), this.pageSize()).subscribe({
      next: (response) => {
        this.pokemonList.set(response.content ?? []);
        this.totalElements.set(response.page?.totalElements ?? 0);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(`Failed to load Pokémon: ${err.message || 'Unknown error'}`, 'Dismiss', {
          duration: 4000,
        });
      },
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadPokemon();
  }

  onSearch(): void {
    const query = this.searchQuery().trim();
    if (!query) {
      this.clearSearch();
      return;
    }

    this.loading.set(true);
    const numId = Number(query);
    if (!isNaN(numId) && numId > 0) {
      // Search by ID
      this.pokemonService.findPokemonByPokedexId(numId).subscribe({
        next: (pokemon) => {
          this.pokemonList.set([pokemon]);
          this.totalElements.set(1);
          this.isSearchActive.set(true);
          this.loading.set(false);
        },
        error: () => {
          this.pokemonList.set([]);
          this.totalElements.set(0);
          this.isSearchActive.set(true);
          this.loading.set(false);
          this.snackBar.open(`Pokémon with Pokédex ID #${numId} not found`, 'Dismiss', {
            duration: 3500,
          });
        },
      });
    } else {
      // Search by Name
      this.pokemonService.findPokemonByName(query.toLowerCase()).subscribe({
        next: (pokemon) => {
          this.pokemonList.set([pokemon]);
          this.totalElements.set(1);
          this.isSearchActive.set(true);
          this.loading.set(false);
        },
        error: () => {
          this.pokemonList.set([]);
          this.totalElements.set(0);
          this.isSearchActive.set(true);
          this.loading.set(false);
          this.snackBar.open(`Pokémon "${query}" not found`, 'Dismiss', {
            duration: 3500,
          });
        },
      });
    }
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.pageIndex.set(0);
    this.loadPokemon();
  }

  openScheduleDialog(): void {
    const dialogRef = this.dialog.open(ScheduleDialogComponent, {
      width: '420px',
    });

    dialogRef.afterClosed().subscribe((generation: Generation | undefined) => {
      if (generation) {
        this.scheduleGeneration(generation);
      }
    });
  }

  scheduleGeneration(generation: Generation): void {
    this.loading.set(true);
    this.pokemonService.enqueuePokemonGeneration(generation).subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar
          .open(`Scheduled sync for ${generation}! Check back in a moment.`, 'Refresh', {
            duration: 5000,
          })
          .onAction()
          .subscribe(() => {
            this.loadPokemon();
          });
        // Auto reload after brief interval to fetch fresh items
        setTimeout(() => this.loadPokemon(), 2000);
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(
          `Failed to schedule ${generation}: ${err.message || 'Unknown error'}`,
          'Dismiss',
          {
            duration: 4000,
          },
        );
      },
    });
  }

  openDetailDialog(pokemon: PokemonDto): void {
    const dialogRef = this.dialog.open(PokemonDetailDialogComponent, {
      data: { pokemon },
      width: '460px',
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result?.action === 'delete') {
        this.deletePokemon(result.pokemon);
      }
    });
  }

  deletePokemon(pokemon: PokemonDto, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    const confirmed = confirm(
      `Are you sure you want to delete ${pokemon.name} (#${pokemon.pokedexId})?`,
    );
    if (!confirmed) return;

    this.loading.set(true);
    this.pokemonService.deletePokemonByPokedexId(pokemon.pokedexId).subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open(`Deleted ${pokemon.name} (#${pokemon.pokedexId})`, 'Dismiss', {
          duration: 3000,
        });
        if (this.isSearchActive()) {
          this.clearSearch();
        } else {
          this.loadPokemon();
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.snackBar.open(
          `Failed to delete Pokémon: ${err.message || 'Unknown error'}`,
          'Dismiss',
          {
            duration: 4000,
          },
        );
      },
    });
  }

  padDexId(id: number): string {
    return String(id).padStart(3, '0');
  }

  formatGeneration(gen?: string): string {
    if (!gen) return 'Unknown';
    if (gen === 'generation-i') return 'Gen I';
    return gen.replace('-', ' ').toUpperCase();
  }

  getPokemonImageUrl(pokemon: PokemonDto): string {
    return this.pokemonService.configuration.basePath + pokemon.imageUrl;
  }
}
