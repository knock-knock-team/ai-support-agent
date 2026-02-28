from __future__ import annotations
from typing import Optional, Dict, Any

from fastapi import HTTPException
from anyio import to_thread

from .config import settings
from agent.qwen_agent import QwenAgent, ModelConfig, InferenceConfig, ServiceConfig

# bring in the existing sentiment helper
from agent.sentiment_classification import SentimentService, SentimentInferenceConfig, SentimentModelConfig, SentimentServiceConfig


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
            # inference_config=inference_conf,
            service_config=service_conf,
        )

        # sentiment classifier lives separately; lazy loading handled inside
        sentiment_device = None  # let SentimentService pick cuda/cpu same as its own logic
        self._sentiment = SentimentService(
            device=sentiment_device,
            model_config=SentimentModelConfig(),
            inference_config=SentimentInferenceConfig(),
            service_config=SentimentServiceConfig(),
        )

    async def generate(self, message: str, context: Optional[str] = None) -> Dict[str, Any]:
        try:
            # the underlying call is CPU/GPU heavy so run in a thread
            result = await to_thread.run_sync(self._agent.generate, message, context)
            response, confidence = result.get('response'), result.get('confidence')
            return {"response": response, "confidence": confidence}
        except Exception as exc:
            raise HTTPException(status_code=500, detail=str(exc))

    async def sentiment(self, text: str, return_proba: bool = False) -> Dict[str, Any]:
        """Run sentiment classification for provided text."""
        try:
            result = await to_thread.run_sync(self._sentiment.predict, text, return_proba)
            return result
        except Exception as exc:
            raise HTTPException(status_code=500, detail=str(exc))

    async def health(self) -> dict:
        # model loading can happen lazily
        agent_info = await to_thread.run_sync(self._agent.health_check)
        sentiment_info = {}
        try:
            sentiment_info = await to_thread.run_sync(self._sentiment.health_check)
        except Exception:
            sentiment_info = {"error": "unavailable"}
        return {"agent": agent_info, "sentiment": sentiment_info}
