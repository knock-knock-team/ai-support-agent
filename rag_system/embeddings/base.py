from __future__ import annotations
from abc import ABC, abstractmethod
from typing import List


class Embedder(ABC):
    @abstractmethod
    def embed(self, texts: List[str]) -> List[List[float]]:
        """Encode a batch of texts into embeddings.

        Args:
            texts: list of raw text strings.
        Returns:
            list of float vectors, one per input text.
        """
        pass
