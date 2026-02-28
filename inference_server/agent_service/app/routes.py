from __future__ import annotations
from fastapi import APIRouter, Depends, HTTPException, Request

from .schemas import GenerateRequest, GenerateResponse, HealthResponse
from .service import AgentService

router = APIRouter()


def get_service(request: Request) -> AgentService:
    # service instance is stored on app state by startup event
    return request.app.state.agent_service


@router.post("/generate", response_model=GenerateResponse)
async def generate(request: GenerateRequest, svc: AgentService = Depends(get_service)) -> GenerateResponse:
    result = await svc.generate(request.message, request.context)
    response, confidence = result.get("response"), result.get("confidence")
    return GenerateResponse(response=response, confidence=confidence)


@router.get("/health", response_model=HealthResponse)
async def health(svc: AgentService = Depends(get_service)) -> HealthResponse:
    info = await svc.health()
    return HealthResponse(status="ok", details=info)
