from __future__ import annotations
from typing import Optional

from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field


_env_path = r"inference_server\agent_service\.env"


class Settings(BaseSettings):
    model_name: str = Field("Qwen/Qwen2.5-3B-Instruct", env="MODEL_NAME")
    use_4bit: bool = Field(True, env="USE_4BIT")
    max_new_tokens: int = Field(512, env="MAX_NEW_TOKENS")
    temperature: float = Field(0.7, env="TEMPERATURE")
    top_p: float = Field(0.8, env="TOP_P")
    repetition_penalty: float = Field(1.1, env="REPETITION_PENALTY")
    context_max_chars: int = Field(4000, env="CONTEXT_MAX_CHARS")

    class Config:
        env_file = _env_path
        env_file_encoding = "utf-8"


settings = Settings()