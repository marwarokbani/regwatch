import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Status } from './status';

describe('Status', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Status],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('affiche chaque service et la chaîne LLM', async () => {
    const fixture = TestBed.createComponent(Status);
    fixture.detectChanges();

    http.expectOne('/health').flush({
      status: 'UP',
      components: { db: { status: 'UP' }, reranker: { status: 'UP' } },
    });
    http.expectOne('/api/llm/providers').flush(['groq (llama)', 'ollama (qwen)']);
    await fixture.whenStable();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('db : UP');
    expect(text).toContain('reranker : UP');
    expect(text).toContain('groq (llama)');
  });

  it("lit le corps d'une réponse 503 quand un service est DOWN", async () => {
    const fixture = TestBed.createComponent(Status);
    fixture.detectChanges();

    http
      .expectOne('/health')
      .flush(
        { status: 'DOWN', components: { ollama: { status: 'DOWN' } } },
        { status: 503, statusText: 'Service Unavailable' },
      );
    http.expectOne('/api/llm/providers').flush([]);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ollama : DOWN');
  });
});
