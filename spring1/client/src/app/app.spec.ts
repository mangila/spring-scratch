import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';
import { PokemonService } from '../api/api/pokemon.service';
import { PokemonDto } from '../api/model/pokemonDto';
import { PagedPokemonDto } from '../api/model/pagedPokemonDto';
import { of } from 'rxjs';

describe('App', () => {
  const mockPokemon: PokemonDto[] = [
    { pokedexId: 1, name: 'bulbasaur', generation: 'generation-i' },
    { pokedexId: 25, name: 'pikachu', generation: 'generation-i' },
  ];

  const mockPagedResponse: PagedPokemonDto = {
    content: mockPokemon,
    page: {
      number: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    },
  };

  let mockPokemonService: Partial<PokemonService>;

  beforeEach(async () => {
    mockPokemonService = {
      findAllPokemon: vi.fn().mockReturnValue(of(mockPagedResponse)),
      findPokemonByPokedexId: vi
        .fn()
        .mockImplementation((id: number) =>
          of(mockPokemon.find((p) => p.pokedexId === id) || mockPokemon[0]),
        ),
      findPokemonByName: vi
        .fn()
        .mockImplementation((name: string) =>
          of(mockPokemon.find((p) => p.name === name) || mockPokemon[0]),
        ),
      deletePokemonByPokedexId: vi.fn().mockReturnValue(of(null)),
      enqueuePokemonGeneration: vi.fn().mockReturnValue(of(null)),
    };

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PokemonService, useValue: mockPokemonService },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render brand and header title', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.app-title')?.textContent).toContain('PokéDex Explorer');
  });

  it('should load and render pokemon list on init', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();

    const app = fixture.componentInstance;
    expect(app.pokemonList().length).toBe(2);
    expect(mockPokemonService.findAllPokemon).toHaveBeenCalledWith(0, 20);

    const compiled = fixture.nativeElement as HTMLElement;
    const cards = compiled.querySelectorAll('.pokemon-card');
    expect(cards.length).toBe(2);
    expect(compiled.textContent).toContain('bulbasaur');
    expect(compiled.textContent).toContain('pikachu');
  });

  it('should toggle between grid and table view modes', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(app.viewMode()).toBe('grid');
    app.viewMode.set('table');
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.pokemon-table')).toBeTruthy();
  });

  it('should perform search by ID', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    app.searchQuery.set('25');
    app.onSearch();

    expect(mockPokemonService.findPokemonByPokedexId).toHaveBeenCalledWith(25);
    expect(app.isSearchActive()).toBe(true);
  });

  it('should perform search by Name', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    app.searchQuery.set('pikachu');
    app.onSearch();

    expect(mockPokemonService.findPokemonByName).toHaveBeenCalledWith('pikachu');
    expect(app.isSearchActive()).toBe(true);
  });

  it('should reset search on clearSearch', async () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    app.searchQuery.set('pikachu');
    app.clearSearch();

    expect(app.searchQuery()).toBe('');
    expect(app.isSearchActive()).toBe(false);
  });
});
