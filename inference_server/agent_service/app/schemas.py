from __future__ import annotations
from typing import Optional

from pydantic import BaseModel, Field


class GenerateRequest(BaseModel):
    message: str = Field(..., description="Input message")
    context: Optional[str] = Field(None, description="Optional context string")


class GenerateResponse(BaseModel):
    response: str = Field(..., description="Generated response")
    confidence: float = Field(..., description="Confidence score for the response")

class HealthResponse(BaseModel):
    status: str
    details: dict


# ---- sentiment-specific models -------------------------------------------

class SentimentRequest(BaseModel):
    text: str = Field(..., description="Text to classify")
    return_proba: bool = Field(False, description="Whether to return probability scores")


class SentimentResponse(BaseModel):
    label: str
    class_id: int
    sentiment_confidence: Optional[float] = Field(
        None, description="Highest class probability when return_proba is True"
    )
