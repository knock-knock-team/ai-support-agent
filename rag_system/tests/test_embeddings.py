from unittest.mock import MagicMock, patch

from rag_system.embeddings.base import Embedder
from rag_system.embeddings.sentence_transformer_embedder import SentenceTransformerEmbedder


def test_embedder_interface():
    class Dummy(Embedder):
        def embed(self, texts):
            return [[len(t)] for t in texts]

    d = Dummy()
    out = d.embed(["a", "bb"])
    assert out == [[1], [2]]


def test_sentence_transformer_embedder(monkeypatch):
    # patch SentenceTransformer to avoid actual model download
    fake_model = MagicMock()
    fake_model.encode.return_value = [[0.1, 0.2]]
    with patch("rag_system.embeddings.sentence_transformer_embedder.SentenceTransformer", return_value=fake_model):
        embedder = SentenceTransformerEmbedder(model_name="test")
        vect = embedder.embed(["hello"])
        assert vect == [[0.1, 0.2]]
        fake_model.encode.assert_called_once_with(["hello"], show_progress_bar=False)
