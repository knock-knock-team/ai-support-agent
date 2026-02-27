from __future__ import annotations
from typing import List, Optional

from pydantic import BaseModel, Field


class DocumentsRequest(BaseModel):
    documents: List[str] = Field(..., description="List of raw document strings")


class SearchRequest(BaseModel):
    query: str = Field(..., description="Query text")
    top_k: Optional[int] = Field(None, description="Number of results to return")


class Match(BaseModel):
    id: str
    text: str
    score: float
    metadata: dict


class SearchResponse(BaseModel):
    matches: List[Match]


class HealthResponse(BaseModel):
    status: str
    details: dict
