# pydantic v2 splits BaseSettings into pydantic-settings
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field

_env_path = '.env'

class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=_env_path if _env_path else ".env",
        env_file_encoding="utf-8"
    )
    
    # optional during tests; real code should set via environment
    qdrant_url: str | None = Field(None, env="QDRANT_URL")
    qdrant_api_key: str | None = Field(None, env="QDRANT_API_KEY")
    embedding_model: str = Field("BAAI/bge-small-en-v1.5", env="EMBEDDING_MODEL")

    chunk_size: int = Field(500, env="CHUNK_SIZE")
    chunk_overlap: int = Field(50, env="CHUNK_OVERLAP")

    default_top_k: int = Field(5, env="DEFAULT_TOP_K")


settings = Settings()