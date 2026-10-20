"""Tests de l'API sans charger le vrai modèle (un faux modèle remplace le CrossEncoder)."""

import pytest
from fastapi.testclient import TestClient

import app as reranker


class FakeCrossEncoder:
    """Score = nombre de mots de la requête présents dans le document."""

    def predict(self, pairs, batch_size=8):
        return [sum(word in doc for word in query.split()) / 10 for query, doc in pairs]


@pytest.fixture
def client(monkeypatch):
    # On neutralise le chargement réel déclenché au démarrage.
    monkeypatch.setattr(reranker, "load_model", lambda: None)
    monkeypatch.setitem(reranker.state, "model", None)
    monkeypatch.setitem(reranker.state, "error", None)
    with TestClient(reranker.app) as c:
        yield c


def test_health_is_503_while_loading(client):
    response = client.get("/health")
    assert response.status_code == 503
    assert response.json()["status"] == "loading"


def test_rerank_is_503_while_loading(client):
    response = client.post("/rerank", json={"query": "délai", "documents": ["a"]})
    assert response.status_code == 503


def test_rerank_sorts_by_score_and_keeps_original_index(client):
    reranker.state["model"] = FakeCrossEncoder()

    response = client.post("/rerank", json={
        "query": "délai de conservation",
        "documents": ["sans rapport", "délai de conservation des données", "délai"],
        "top_n": 2,
    })

    assert response.status_code == 200
    assert [r["index"] for r in response.json()["results"]] == [1, 2]
    assert client.get("/health").json()["status"] == "ready"


def test_rejects_empty_documents(client):
    reranker.state["model"] = FakeCrossEncoder()
    response = client.post("/rerank", json={"query": "x", "documents": []})
    assert response.status_code == 422
