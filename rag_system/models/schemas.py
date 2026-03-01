from __future__ import annotations
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, Field


class Document(BaseModel):
    id: str
    text: str
    metadata: Dict[str, Any] = Field(default_factory=dict)


class Chunk(BaseModel):
    id: str
    document_id: str
    text: str
    metadata: Dict[str, Any] = Field(default_factory=dict)


class RetrievalResult(BaseModel):
    chunk: Chunk
    score: float
    # placeholder for any reranking-related info in future
    rerank_score: Optional[float] = None
