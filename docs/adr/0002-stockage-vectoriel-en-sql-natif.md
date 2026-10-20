# ADR 0002 — Stockage vectoriel en SQL natif (table `chunk` + pgvector)

- **Statut** : Accepté (M1, mis en œuvre au M3–M4)
- **Date** : 2026-10-20

## Contexte

La recherche doit être **hybride** : similarité vectorielle (pgvector) + plein texte PostgreSQL (`tsvector`),
fusionnées par Reciprocal Rank Fusion, puis rerankées. Chaque passage retrouvé doit être relié à son
document et à sa section par des **clés étrangères** : les citations du LLM sont vérifiées contre des chunks réels
(anti-hallucination) et le rapport d'impact pointe vers la procédure à modifier.

## Décision

- Une table `chunk` définie par nous (migration Flyway) avec : `document_id` (FK), `section_path`, `section_ref`,
  `content`, `embedding vector(1024)` et une colonne générée `content_tsv tsvector` (configuration `french`).
- Index **HNSW** (`vector_cosine_ops`) pour le vectoriel, **GIN** pour le plein texte.
- La recherche hybride est **une seule requête SQL** (CTE vectorielle + CTE plein texte + fusion RRF), exécutée
  via `JdbcClient`. Les entités métier (documents, analyses, verdicts) restent en JPA.
- Spring AI n'est utilisé que pour le calcul des embeddings (`EmbeddingModel`).

## Alternatives écartées

| Option | Pourquoi écartée |
|---|---|
| `PgVectorStore` de Spring AI | Table imposée, métadonnées en JSON sans clé étrangère, pas de recherche plein texte ni hybride : il faudrait de toute façon écrire le SQL à côté. |
| Base vectorielle dédiée (Qdrant, Weaviate, Chroma) | Un service de plus à faire tourner (RAM limitée), deux sources de vérité à synchroniser, pas de jointure avec les données métier. |
| Elasticsearch / OpenSearch pour le plein texte | Très lourd en mémoire pour quelques centaines de chunks ; PostgreSQL gère le français (stemming) nativement. |
| Tout en JPA (Hibernate Vector) | Les requêtes de classement (CTE, `ts_rank`, opérateur `<=>`, fusion) s'expriment mal en JPQL ; le SQL natif reste lisible et testable. |

## Conséquences

- Une seule base, une seule sauvegarde, des transactions communes entre données métier et vecteurs.
- La requête hybride est explicite et chaque score intermédiaire est exposé (endpoint de debug au M4).
- Le SQL est spécifique à PostgreSQL : assumé, la portabilité n'est pas un objectif.
- Les tests d'intégration utilisent la même image `pgvector/pgvector` (Testcontainers), pas une base en mémoire.
