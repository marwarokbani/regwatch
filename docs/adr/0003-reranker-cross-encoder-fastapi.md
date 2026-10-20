# ADR 0003 — Reranking par cross-encoder dans un micro-service FastAPI

- **Statut** : Accepté (M1, utilisé au M4)
- **Date** : 2026-10-20

## Contexte

La recherche hybride compare des embeddings calculés **séparément** pour la requête et le passage (bi-encoder) :
c'est rapide mais approximatif. Un **cross-encoder** lit la paire (exigence, passage) d'un seul bloc et produit un
score de pertinence bien plus fiable, au prix d'un calcul par paire. On l'applique donc seulement aux ~20 meilleurs
candidats de la recherche hybride.

Spring AI 1.1 ne fournit **aucune implémentation de cross-encoder** : il n'a qu'une interface de post-traitement
de documents, sans modèle derrière. Les documents sont en français : le modèle doit être multilingue.

## Décision

- Modèle **`BAAI/bge-reranker-v2-m3`** : multilingue (même famille que l'embedding `bge-m3`), open source, utilisable sur CPU.
- Servi par un **micro-service Python FastAPI** (`reranker/app.py`, environ 100 lignes) avec `sentence-transformers`
  (`CrossEncoder`) et torch en version **CPU**.
- API : `POST /rerank {query, documents[], top_n}` → `[{index, score}]` triés ; `GET /health` → 503 tant que le
  modèle charge (le premier démarrage télécharge environ 2,3 Go dans un volume Docker).
- Côté Java, l'appel passera par un port `Reranker` (M4) : remplacer l'implémentation ne touche pas au pipeline.

## Alternatives écartées

| Option | Pourquoi écartée |
|---|---|
| `bge-reranker-base` / `large` | Entraînés surtout sur l'anglais et le chinois : moins bons sur des textes réglementaires français. |
| Hugging Face Text Embeddings Inference (TEI) | Serveur Rust performant et prêt à l'emploi, mais boîte noire. Reste un remplaçant crédible derrière le port `Reranker`. |
| ONNX Runtime / DJL dans la JVM | Tokenizer et post-traitement à reproduire en Java, plus de complexité et un risque d'écart avec le modèle de référence. |
| LLM comme reranker (via Groq) | Consomme le quota gratuit (20 passages × N exigences), plus lent, scores moins stables. |
| Pas de reranking | C'est justement une des configurations comparées au M7 (vectoriel / hybride / hybride + reranking). |

## Conséquences

- Un conteneur Python de plus (environ 2,5 à 3 Go de RAM une fois le modèle chargé), d'où la limite mémoire et l'ADR 0004.
- Latence CPU de l'ordre de la seconde à quelques secondes pour 20 paires : acceptable pour une analyse en arrière-plan.
- Le service est testé sans le vrai modèle (`test_app.py`, faux `CrossEncoder`), ce qui rend la CI légère.
