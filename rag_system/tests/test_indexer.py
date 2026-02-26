from unittest.mock import MagicMock

from rag_system.indexing.indexer import Indexer
from rag_system.models.schemas import Document
from rag_system.preprocessing.chunker import Chunker


def test_indexer_calls_embed_and_upsert(monkeypatch):
    # prepare mocks
    fake_embedder = MagicMock()
    fake_embedder.embed.return_value = [[0.1, 0.2]]
    fake_store = MagicMock()
    docs = [Document(id="1", text="hello world", metadata={"foo": "bar"})]
    # use real chunker with small chunk size to create one chunk
    chunker = Chunker(chunk_size=10, chunk_overlap=0)

    indexer = Indexer(embedder=fake_embedder, vector_store=fake_store, chunker=chunker)
    indexer.index_documents(docs)

    # verify embed called with chunk text
    fake_embedder.embed.assert_called()
    fake_store.upsert.assert_called()
    called_vectors = fake_store.upsert.call_args[1]["vectors"]
    called_ids = fake_store.upsert.call_args[1]["ids"]
    assert called_vectors == [[0.1, 0.2]]
    assert called_ids[0].startswith("1-")
