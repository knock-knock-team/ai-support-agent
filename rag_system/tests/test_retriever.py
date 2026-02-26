from typing import List
from unittest.mock import MagicMock

from rag_system.retrieval.retriever import Retriever


def test_retriever_flow(monkeypatch):
    fake_embedder = MagicMock()
    fake_embedder.embed.return_value = [[0.5, 0.5]]
    fake_store = MagicMock()
    fake_store.search.return_value = [
        {"id": "1", "score": 0.9, "metadata": {"source_document_id": "doc1", "text": "chunk text"}}
    ]
    retriever = Retriever(embedder=fake_embedder, vector_store=fake_store)
    results = retriever.retrieve("some query", top_k=3, filters={"foo": "bar"})
    # verify embedding call and search param propagation
    fake_embedder.embed.assert_called_with(["some query"])
    fake_store.search.assert_called()
    assert len(results) == 1
    assert results[0].chunk.text == "chunk text"
    assert results[0].score == 0.9


def test_qdrant_filter_builder(monkeypatch):
    from rag_system.vectorstore import qdrant_store

    # monkeypatch QdrantClient to avoid network operations
    class DummyClient:
        def __init__(self, **kwargs):
            pass

        def get_collection(self, name):
            raise Exception("not found")

        def create_collection(self, collection_name, vectors_config):
            pass

    monkeypatch.setattr(qdrant_store, "QdrantClient", DummyClient)
    store = qdrant_store.QdrantVectorStore(collection_name="test")
    filt = store._build_filter({"a": "b", "c": 1})
    # filter must include two must conditions
    assert len(filt.must) == 2
    keys = {cond.key for cond in filt.must}
    assert keys == {"a", "c"}
