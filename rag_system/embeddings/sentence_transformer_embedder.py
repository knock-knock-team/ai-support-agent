from __future__ import annotations
from typing import List

from sentence_transformers import SentenceTransformer

from .base import Embedder
from ..config import settings
from ..utils.logging import logger


class SentenceTransformerEmbedder(Embedder):
    def __init__(self, model_name: str | None = None):
        self.model_name = model_name or settings.embedding_model
        logger.info(f"Loading embedding model {self.model_name}")
        self.model = SentenceTransformer(self.model_name)

    def embed(self, texts: List[str]) -> List[List[float]]:
        logger.debug(f"Embedding {len(texts)} texts")
        vect = self.model.encode(texts, show_progress_bar=False)
        # the underlying model may return numpy array or list
        try:
            return vect.tolist()
        except AttributeError:
            return list(vect)
