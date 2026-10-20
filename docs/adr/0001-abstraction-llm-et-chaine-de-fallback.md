# ADR 0001 — Abstraction LLM et chaîne de fallback

- **Statut** : Accepté (M1)
- **Date** : 2026-10-20

## Contexte

RegWatch doit rester 100 % gratuit. Les API gratuites (Groq, Gemini via Google AI Studio) imposent des quotas
par minute et par jour : une analyse complète (≈ 15 exigences × 1 appel « juge ») ou une campagne d'évaluation
peut les dépasser. Il faut donc pouvoir :

1. changer de fournisseur par configuration, sans toucher au code métier ;
2. basculer automatiquement vers un autre fournisseur quand l'un est saturé ou indisponible ;
3. tracer quel fournisseur/modèle a produit chaque verdict (exigence d'auditabilité dans un contexte bancaire).

## Décision

- Le code métier dépend uniquement d'un **port** `LlmClient` (`chat(LlmRequest) → LlmResponse`).
  `LlmResponse` contient le contenu **et** le fournisseur, le modèle, la latence et les tokens consommés.
- Chaque fournisseur est un `LlmProvider` ; une seule implémentation, `ChatModelLlmProvider`, adapte
  n'importe quel `ChatModel` de **Spring AI**.
- **Groq et Gemini** passent tous deux par le module OpenAI de Spring AI, car ils exposent une API compatible
  OpenAI (`/chat/completions`) ; seuls `base-url`, `completions-path`, clé et modèle diffèrent.
  **Ollama** passe par le module Ollama natif.
- `FallbackLlmClient` (le `LlmClient` injecté) essaie les fournisseurs dans l'ordre de `LLM_CHAIN`
  (défaut `groq,gemini,ollama`) et renvoie la première réponse. Si tous échouent :
  `LlmUnavailableException` → HTTP 503 avec la liste des échecs.
- Un fournisseur compatible OpenAI **sans clé API est ignoré** au démarrage (avec un warning) ; une chaîne
  vide fait échouer le démarrage (fail fast).
- Le `RetryTemplate` par défaut de Spring AI (10 tentatives, backoff jusqu'à 3 min) est remplacé par un
  retry court (2 tentatives, 1 s) limité aux erreurs transitoires (5xx, réseau). Spring AI classe les 4xx,
  **dont le 429 « quota dépassé »**, comme non transitoires : la bascule est donc immédiate.
- Les modules Spring AI « bruts » sont utilisés plutôt que les *starters* : l'auto-configuration des starters
  ne crée qu'un `ChatModel` par type, alors qu'on en a deux de type OpenAI (Groq et Gemini).

## Alternatives écartées

| Option | Pourquoi écartée |
|---|---|
| Utiliser directement `ChatModel`/`ChatClient` de Spring AI dans le métier | Pas de point unique pour le fallback ni la traçabilité du fournisseur ; le métier dépendrait d'un framework. |
| Interface maison + clients HTTP écrits à la main | Réécrit ce que Spring AI fait déjà (formats d'API, gestion d'erreurs, observabilité). |
| Module Spring AI `vertex-ai-gemini` | Exige un projet Google Cloud avec facturation : incompatible avec la contrainte « sans carte bancaire ». |
| Fallback par profil Spring (un seul fournisseur actif) | Pas de bascule automatique en cas de quota atteint en pleine analyse. |
| Circuit breaker (Resilience4j) dès le M1 | Prématuré. Un rate limiter côté client sera ajouté au M5, quand le volume d'appels sera réel. |

## Conséquences

- Ajouter un fournisseur compatible OpenAI (Mistral, OpenRouter…) = quelques lignes de configuration.
- Les verdicts d'une même analyse peuvent provenir de modèles différents si une bascule a lieu en cours de route ;
  c'est pourquoi le fournisseur et le modèle sont persistés avec chaque verdict (M5) et affichés.
- Le fallback local (Ollama, modèle 3B) est nettement moins bon que les modèles cloud : il garantit la
  continuité de service, pas la qualité. Limite documentée.
