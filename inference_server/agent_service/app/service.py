from __future__ import annotations
from typing import Optional

from fastapi import HTTPException
from anyio import to_thread

from .config import settings
from agent.qwen_agent import QwenAgent, ModelConfig, InferenceConfig, ServiceConfig


class AgentService:
    def __init__(self) -> None:
        model_conf = ModelConfig(model_name=settings.model_name, use_4bit=settings.use_4bit)
        inference_conf = InferenceConfig(
            max_new_tokens=settings.max_new_tokens,
            temperature=settings.temperature,
            top_p=settings.top_p,
            repetition_penalty=settings.repetition_penalty,
        )
        service_conf = ServiceConfig(context_max_chars=settings.context_max_chars)
        self._agent = QwenAgent(
            model_config=model_conf,
            inference_config=inference_conf,
            service_config=service_conf,
        )

    async def generate(self, message: str, context: Optional[str] = None) -> str:
        try:
            # the underlying call is CPU/GPU heavy so run in a thread
            result = await to_thread.run_sync(self._agent.generate, message, context)
            return result
        except Exception as exc:
            raise HTTPException(status_code=500, detail=str(exc))

    async def health(self) -> dict:
        # model loading can happen lazily
        info = await to_thread.run_sync(self._agent.health_check)
        return info
