from __future__ import annotations
from typing import Optional

from pydantic import BaseModel, Field


class GenerateRequest(BaseModel):
    message: str = Field(..., description="Input message")
    context: Optional[str] = Field(None, description="Optional context string")


class GenerateResponse(BaseModel):
    response: str = Field(..., description="Generated response")


class HealthResponse(BaseModel):
    status: str
    details: dict
