from __future__ import annotations
from typing import List

from fastapi import APIRouter, Depends, Request

from .schemas import DocumentsRequest, SearchRequest, SearchResponse, Match, HealthResponse
from .service import RagService

router = APIRouter()


def get_service(request: Request) -> RagService:
    # service instance is created at application startup and stored in state
    return request.app.state.rag_service


@router.post("/documents", status_code=204)
async def add_documents(request: DocumentsRequest, svc: RagService = Depends(get_service)) -> None:
    await svc.add_documents(request.documents)


@router.post("/search", response_model=SearchResponse)
async def search(request: SearchRequest, svc: RagService = Depends(get_service)) -> SearchResponse:
    results = await svc.search(request.query, request.top_k)
    matches: List[Match] = []
    for rr in results:
        matches.append(
            Match(
                id=rr.chunk.id,
                text=rr.chunk.text,
                score=rr.score,
                metadata=rr.chunk.metadata or {},
            )
        )
    return SearchResponse(matches=matches)


@router.get("/health", response_model=HealthResponse)
async def health(svc: RagService = Depends(get_service)) -> HealthResponse:
    info = await svc.health()
    return HealthResponse(status="ok", details=info)
