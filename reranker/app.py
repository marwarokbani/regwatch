"""Micro-service de reranking pour RegWatch.

Un cross-encoder lit la paire (requête, passage) d'un seul bloc et produit un score de
pertinence, plus précis que la similarité cosinus entre deux embeddings calculés séparément.
On ne l'applique donc qu'aux ~20 meilleurs candidats issus de la recherche hybride.
"""

import logging
import os
import threading
import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Response
from pydantic import BaseModel, Field

MODEL_NAME = os.getenv("RERANKER_MODEL", "BAAI/bge-reranker-v2-m3")
MAX_LENGTH = int(os.getenv("RERANKER_MAX_LENGTH", "512"))
BATCH_SIZE = int(os.getenv("RERANKER_BATCH_SIZE", "8"))

log = logging.getLogger("reranker")
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s - %(message)s")

# État partagé entre le thread de chargement et les requêtes HTTP.
state: dict = {"model": None, "error": None}


def load_model() -> None:
    try:
        # Import tardif : torch est long à importer, /health doit répondre immédiatement.
        from sentence_transformers import CrossEncoder

        start = time.perf_counter()
        state["model"] = CrossEncoder(MODEL_NAME, max_length=MAX_LENGTH, device="cpu")
        log.info("Modèle %s chargé en %.1f s", MODEL_NAME, time.perf_counter() - start)
    except Exception as exc:  # noqa: BLE001 - on veut exposer toute erreur via /health
        log.exception("Échec du chargement du modèle")
        state["error"] = str(exc)


@asynccontextmanager
async def lifespan(_: FastAPI):
    # Au premier démarrage, le modèle (~2,3 Go) est téléchargé : on charge en arrière-plan
    # pour que /health puisse répondre "loading" au lieu de faire attendre le conteneur.
    threading.Thread(target=load_model, daemon=True).start()
    yield


app = FastAPI(title="RegWatch Reranker", lifespan=lifespan)


class RerankRequest(BaseModel):
    query: str = Field(min_length=1)
    documents: list[str] = Field(min_length=1, max_length=100)
    top_n: int | None = Field(default=None, ge=1)


class RerankResult(BaseModel):
    index: int  # position du document dans la requête d'origine
    score: float  # pertinence dans [0, 1] (sigmoïde appliquée par le CrossEncoder)


class RerankResponse(BaseModel):
    model: str
    results: list[RerankResult]
    latency_ms: int


@app.get("/health")
def health(response: Response) -> dict:
    if state["model"] is not None:
        return {"status": "ready", "model": MODEL_NAME}
    response.status_code = 503
    if state["error"]:
        return {"status": "error", "model": MODEL_NAME, "error": state["error"]}
    return {"status": "loading", "model": MODEL_NAME}


# Fonction synchrone : FastAPI l'exécute dans un pool de threads, la boucle async reste libre
# pendant l'inférence (CPU-bound).
@app.post("/rerank", response_model=RerankResponse)
def rerank(request: RerankRequest) -> RerankResponse:
    model = state["model"]
    if model is None:
        raise HTTPException(status_code=503, detail="Modèle en cours de chargement")

    start = time.perf_counter()
    scores = model.predict([(request.query, doc) for doc in request.documents], batch_size=BATCH_SIZE)
    ranked = sorted(enumerate(scores), key=lambda pair: pair[1], reverse=True)
    if request.top_n is not None:
        ranked = ranked[: request.top_n]

    return RerankResponse(
        model=MODEL_NAME,
        results=[RerankResult(index=i, score=float(s)) for i, s in ranked],
        latency_ms=int((time.perf_counter() - start) * 1000),
    )
