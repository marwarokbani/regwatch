# ADR 0004 — Profils Docker Compose et budget mémoire

- **Statut** : Accepté (M1)
- **Date** : 2026-10-20

## Contexte

Machine de développement : 16 Go de RAM, GPU RTX 2050 (4 Go de VRAM), Docker Desktop sur WSL2.
Le LLM de fallback local est le composant le plus gourmand et n'est utile que si les API cloud sont indisponibles.

## Décision

- **Stack par défaut** (`docker compose up -d`) : `postgres`, `ollama` (embeddings `bge-m3` uniquement),
  `reranker` (CPU), `backend`, `frontend`.
- **Profil `local-llm`** (`docker compose --profile local-llm up -d`) : ajoute `ollama-llm`, une instance
  Ollama **séparée** avec le modèle de chat (`qwen2.5:3b` par défaut).
- Les modèles sont téléchargés par des conteneurs éphémères `*-init` (`ollama pull`), pas au build.
- Limites mémoire par service :

| Service | Limite | Contenu |
|---|---|---|
| postgres | 512 Mo | quelques milliers de chunks au plus |
| ollama | 2 Go | bge-m3 (~1,2 Go) |
| reranker | 3 Go | bge-reranker-v2-m3 en fp32 (~2,3 Go) + torch |
| backend | 1 Go | JVM (`MaxRAMPercentage=75`) |
| frontend | 128 Mo | nginx |
| **Total par défaut** | **≈ 6,6 Go** | |
| ollama-llm (profil) | 5 Go | modèle 3B quantifié + contexte |

## Alternatives écartées

| Option | Pourquoi écartée |
|---|---|
| Une seule instance Ollama pour embeddings + chat | Une seule limite mémoire pour deux usages : le chargement du LLM pourrait évincer bge-m3 en pleine ingestion. Deux instances = isolation claire et profil activable indépendamment. |
| Télécharger les modèles au build des images | Images de plusieurs Go, rebuild lent, et téléchargement à chaque modification de code. |
| Fichier `docker-compose.override.yml` pour le LLM local | Les profils sont le mécanisme natif de Compose pour les services optionnels ; un seul fichier à lire. |

## Conséquences

- `.wslconfig` recommandé : `memory=8GB`, `swap=4GB` (voir le README). La stack par défaut tient dans ses
  ~6,6 Go de limites, qui sont des plafonds et non une consommation réelle. Le profil `local-llm` n'est à
  activer que lorsque les API cloud sont indisponibles.
- Leçon tirée du M1 : avec WSL à 8 Go, un autre projet Docker en parallèle et un navigateur chargé, Windows
  est descendu à 0,7 Go de RAM libre. Le fichier d'échange a grossi de 5,5 Go et a saturé le disque, ce qui a
  fait planter Docker. On n'augmente donc pas la mémoire de WSL, et on évite de faire tourner un autre projet
  Docker en même temps.
- Le GPU n'est pas activé par défaut (configuration commentée dans `docker-compose.yml`) : il demande les pilotes
  NVIDIA côté Windows et n'apporte un gain que pour `ollama-llm`.
