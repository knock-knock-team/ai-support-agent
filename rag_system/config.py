# pydantic v2 splits BaseSettings into pydantic-settings
import os
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field

# find .env by searching upward from this file
_env_path = None
_current = os.path.dirname(os.path.abspath(__file__))
_debug_lines = [f"Starting from: {_current}"]
for i in range(3):  # search up to 3 levels up
    _candidate = os.path.join(_current, ".env")
    _debug_lines.append(f"Checking: {_candidate} exists={os.path.isfile(_candidate)}")
    if os.path.isfile(_candidate):
        _env_path = _candidate
        _debug_lines.append(f"Found .env at: {_env_path}")
        break
    _current = os.path.dirname(_current)
if not _env_path:
    _debug_lines.append(".env not found, will use default")
_debug_output = "\n".join(_debug_lines)
_debug_file = os.path.join(os.path.dirname(__file__), "..", "config_debug.txt")
try:
    with open(_debug_file, "w") as f:
        f.write(_debug_output)
except:
    pass  # ignore debug file write errors


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

# Debug: append what was loaded
_debug_lines.append(f"\n--- Settings loaded ---")
_debug_lines.append(f"qdrant_url: {settings.qdrant_url}")
_debug_lines.append(f"qdrant_api_key: {bool(settings.qdrant_api_key)}")  # don't expose full key
_debug_lines.append(f"embedding_model: {settings.embedding_model}")

_debug_output = "\n".join(_debug_lines)
try:
    with open(_debug_file, "w") as f:
        f.write(_debug_output)
except:
    pass  # ignore debug file write errors