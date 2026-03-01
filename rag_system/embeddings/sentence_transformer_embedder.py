from __future__ import annotations
from typing import List

import torch
from transformers import AutoTokenizer, AutoModel

from .base import Embedder
from ..config import settings
from ..utils.logging import logger


class SentenceTransformerEmbedder(Embedder):
    def __init__(self, model_name: str | None = None):
        self.model_name = model_name or settings.embedding_model
        logger.info(f"Loading embedding model {self.model_name}")
        self.tokenizer = AutoTokenizer.from_pretrained(self.model_name)
        self.model = AutoModel.from_pretrained(self.model_name)

        self.device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
        self.model.to(self.device)
        self.model.eval()

    def embed(self, texts: List[str]) -> List[List[float]]:
        logger.debug(f"Embedding {len(texts)} texts")
        with torch.no_grad():
            encodings = self.tokenizer(
                texts,
                padding=True,
                truncation=True,
                return_tensors="pt"
            )
            input_ids = encodings["input_ids"].to(self.device)
            attention_mask = encodings["attention_mask"].to(self.device)

            outputs = self.model(input_ids=input_ids, attention_mask=attention_mask)
            last_hidden_state = outputs.last_hidden_state

            mask = attention_mask.unsqueeze(-1)
            sum_hidden = (last_hidden_state * mask).sum(dim=1)
            lengths = mask.sum(dim=1).clamp(min=1)
            embeddings = sum_hidden / lengths

            return embeddings.cpu().tolist()