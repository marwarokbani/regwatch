# Architecture Decision Records

Chaque décision structurante est consignée dans un ADR court : contexte, décision, alternatives écartées, conséquences.
Un ADR accepté n'est pas modifié : s'il est remplacé, un nouvel ADR le marque « Remplacé par ».

| # | Décision | Statut |
|---|----------|--------|
| [0001](0001-abstraction-llm-et-chaine-de-fallback.md) | Port `LlmClient` au-dessus de Spring AI, chaîne de fallback Groq → Gemini → Ollama | Accepté |
| [0002](0002-stockage-vectoriel-en-sql-natif.md) | Table `chunk` maison + SQL natif plutôt que `PgVectorStore` | Accepté |
| [0003](0003-reranker-cross-encoder-fastapi.md) | Reranking par cross-encoder `bge-reranker-v2-m3` dans un micro-service FastAPI | Accepté |
| [0004](0004-profils-compose-et-budget-memoire.md) | Profils Docker Compose et budget mémoire | Accepté |
| [0005](0005-embeddings-bge-m3-via-ollama.md) | Embeddings `bge-m3` servis par Ollama | Accepté |
