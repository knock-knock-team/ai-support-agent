from __future__ import annotations
from fastapi import FastAPI, Request
from contextlib import asynccontextmanager

from .routes import router
from .service import RagService
from .config import settings


def create_app() -> FastAPI:
    app = FastAPI(title="RAG Service")

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        # startup: instantiate service
        app.state.rag_service = RagService(settings.embedding_model_onnx, settings.embedding_model_onnx_tokenizer)
        try:
            yield
        finally:
            # shutdown cleanup if necessary
            pass

    app.router.lifespan_context = lifespan

    app.include_router(router)
    return app


app = create_app()
