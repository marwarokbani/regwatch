# RegWatch

**Analyse d'impact réglementaire bancaire par RAG.** Quand la Banque Centrale publie une circulaire, RegWatch
en extrait les exigences atomiques, retrouve les passages des procédures internes concernés et juge, pour chaque
exigence, si la banque est `CONFORME`, `PARTIEL`, `NON_CONFORME` ou `NON_COUVERT`. Les cas à faible confiance
partent en revue humaine. Le résultat est un rapport d'impact.

> Projet portfolio — données 100 % synthétiques (banque fictive « Banque Atlas »), stack 100 % gratuite.

🚧 En cours de construction — jalon actuel : **M1 (fondations)**.

## Stack

| Couche | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring AI 1.1, Spring Data JPA, Flyway |
| Base | PostgreSQL 17 + pgvector 0.8 (données métier et vecteurs dans la même base) |
| LLM | Groq → Gemini → Ollama (chaîne de fallback configurable) |
| Embeddings | `bge-m3` via Ollama (local) |
| Reranking | `bge-reranker-v2-m3` via un micro-service FastAPI (local, CPU) |
| Frontend | Angular 22, Angular Material, nginx |
| Infra | Docker Compose, GitHub Actions |

Les choix structurants sont justifiés dans [`docs/adr/`](docs/adr/README.md).

## Démarrage rapide

### Prérequis

- Docker Desktop (WSL2) avec **au moins 8 Go** alloués à WSL (voir [Configuration WSL](#configuration-wsl-recommandée))
- Une clé API gratuite, sans carte bancaire : [Groq](https://console.groq.com/keys) et/ou
  [Google AI Studio](https://aistudio.google.com/apikey)

### Lancement

```bash
cp .env.example .env          # puis renseigner GROQ_API_KEY et/ou GEMINI_API_KEY
docker compose up -d --build
```

Le **premier** démarrage télécharge environ 4 Go de modèles (bge-m3 + reranker), stockés dans des volumes Docker
et réutilisés ensuite. Suivi : `docker compose logs -f ollama-init reranker`.

| URL | Rôle |
|---|---|
| http://localhost:4200 | Interface web |
| http://localhost:8080/health | État du backend et des services (db, ollama, reranker) |
| http://localhost:8080/health/liveness | Probe de vie (ne dépend que de la JVM) |

> Le port 8080 est déjà pris sur la machine (`curl` renvoie une page `401 Unauthorized` qui ne vient pas de
> RegWatch, typiquement le listener d'Oracle XE) ? Mettre `BACKEND_PORT=8081` dans `.env` et adapter les URL.

### Tester la chaîne LLM

```bash
curl http://localhost:8080/api/llm/providers
curl -X POST http://localhost:8080/api/llm/test \
     -H "Content-Type: application/json" \
     -d '{"prompt": "Cite une obligation KYC en une phrase."}'
```

La réponse indique quel fournisseur a répondu (`provider`, `model`), la latence et les tokens consommés.

### LLM local (optionnel)

Le fallback Ollama ne démarre qu'avec le profil `local-llm` (il demande environ 3 à 5 Go de RAM en plus) :

```bash
docker compose --profile local-llm up -d
```

Sans clé API, mettre `LLM_CHAIN=ollama` dans `.env` pour n'utiliser que le modèle local.

## Configuration WSL recommandée

Compléter `%UserProfile%\.wslconfig`, puis exécuter `wsl --shutdown` et relancer Docker Desktop.
Sur une machine à 16 Go, garder **8 Go** pour WSL : Windows, le navigateur et l'IDE ont besoin du reste,
sinon le fichier d'échange de Windows grossit et peut saturer le disque.

```ini
[wsl2]
memory=8GB       # plafond de RAM de la VM WSL
processors=8
swap=4GB

[experimental]
autoMemoryReclaim=gradual   # rend progressivement à Windows la RAM libérée par la VM
```

Détail du budget mémoire par service : [ADR 0004](docs/adr/0004-profils-compose-et-budget-memoire.md).

## Développement

```bash
# Backend (JDK 21) — tests unitaires, puis tests d'intégration (Testcontainers, Docker requis)
cd backend && ./mvnw test && ./mvnw verify

# Reranker
cd reranker && pip install -r requirements-dev.txt && pytest

# Frontend (Node >= 24.15 ou >= 22.22.3)
cd frontend && npm ci && npm test && npm start
```

Pour lancer le backend hors Docker contre la stack Compose : `docker compose up -d postgres ollama reranker`,
puis `./mvnw spring-boot:run` (les URL par défaut pointent vers `localhost`).

## Structure

```
backend/    API Spring Boot (package par fonctionnalité : llm, document, ingestion, retrieval…)
reranker/   micro-service FastAPI de reranking
frontend/   application Angular
eval/       scripts d'évaluation (M7)
data/       procédures, circulaires et vérité terrain synthétiques (M2)
docs/       architecture et ADR
```

## Feuille de route

- [x] M1 — Fondations (Compose, abstraction LLM, health)
- [ ] M2 — Données synthétiques
- [ ] M3 — Ingestion & indexation
- [ ] M4 — Recherche hybride + reranking
- [ ] M5 — Extraction d'exigences & jugement
- [ ] M6 — Frontend Angular
- [ ] M7 — Évaluation
- [ ] M8 — CI/CD & finitions
