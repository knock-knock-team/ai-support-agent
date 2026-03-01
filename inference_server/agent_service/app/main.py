from __future__ import annotations
from fastapi import FastAPI, Request
from contextlib import asynccontextmanager

from .routes import router
from .service import AgentService


def create_app() -> FastAPI:
    app = FastAPI(title="Agent Service")

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        # startup logic
        app.state.agent_service = AgentService()
        try:
            yield
        finally:
            # shutdown logic (if cleanup required)
            pass

    app.router.lifespan_context = lifespan  # assign lifespan context

    # dependency factory kept in routes, no need to redefine here
    app.include_router(router)
    return app


app = create_app()
