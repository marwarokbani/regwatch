# ADR 0005 — Embeddings `bge-m3` servis par Ollama

- **Statut** : Accepté (M1, utilisé au M3)
- **Date** : 2026-10-20

## Contexte

Les embeddings doivent être calculés localement (gratuité, confidentialité des procédures internes d'une banque),
en **français**, sur des passages réglementaires pouvant dépasser 512 tokens.

## Décision

- Modèle **`bge-m3`** (BAAI) : multilingue, vecteurs de dimension **1024**, contexte jusqu'à 8192 tokens.
- Servi par **Ollama** (modèle disponible tel quel dans sa bibliothèque), consommé côté Java par
  `OllamaEmbeddingModel` de Spring AI.

## Alternatives écartées

| Option | Pourquoi écartée |
|---|---|
| Service Python `sentence-transformers` | Possible (on a déjà un service Python), mais Ollama le fait sans code et Spring AI s'y connecte directement. |
| `nomic-embed-text`, `all-MiniLM` | Plus légers, mais nettement moins bons en français. |
| `multilingual-e5-large` | Bon candidat, mais limité à 512 tokens et absent du catalogue Ollama officiel. |
| API d'embeddings cloud | Contraire à la contrainte de gratuité et à la confidentialité des procédures internes. |

## Conséquences

- La colonne `chunk.embedding` est typée `vector(1024)`. Changer de modèle d'embedding impose une migration
  et une réindexation complète.
- bge-m3 produit aussi des représentations creuses (« sparse ») qu'Ollama n'expose pas. Le plein texte
  PostgreSQL joue ce rôle lexical dans la recherche hybride.
