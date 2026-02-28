import os
import logging
import threading
import torch

from dataclasses import dataclass
from typing import Optional, Dict, Any

from transformers import (
    AutoTokenizer,
    AutoModelForSequenceClassification
)

# =========================================================
# Logging
# =========================================================

logger = logging.getLogger("SentimentService")


# =========================================================
# Configs
# =========================================================

@dataclass
class SentimentModelConfig:
    model_name: str = "blanchefort/rubert-base-cased-sentiment"


@dataclass
class SentimentInferenceConfig:
    max_length: int = 512


@dataclass
class SentimentServiceConfig:
    cache_dir: Optional[str] = None


# =========================================================
# Sentiment Service
# =========================================================

class SentimentService:
    _model = None
    _tokenizer = None
    _load_lock = threading.Lock()
    _predict_lock = threading.Lock()

    def __init__(
        self,
        device: Optional[str] = None,
        model_config: Optional[SentimentModelConfig] = None,
        inference_config: Optional[SentimentInferenceConfig] = None,
        service_config: Optional[SentimentServiceConfig] = None,
    ):
        self.device = device or (
            "cuda" if torch.cuda.is_available() else "cpu"
        )

        self.model_config = model_config or SentimentModelConfig()
        self.inference_config = inference_config or SentimentInferenceConfig()
        self.service_config = service_config or SentimentServiceConfig()

        self.cache_dir = self.service_config.cache_dir or os.path.join(
            os.path.dirname(os.path.abspath(__file__)),
            "models_cache"
        )

        self._loaded = False

    # =========================================================
    # Model Lifecycle
    # =========================================================

    def load(self):

        if self._loaded:
            return

        with self._load_lock:

            if self._loaded:
                return

            logger.info(f"Loading sentiment model {self.model_config.model_name}")

            os.makedirs(self.cache_dir, exist_ok=True)

            try:
                tokenizer = AutoTokenizer.from_pretrained(
                    self.model_config.model_name,
                    cache_dir=self.cache_dir
                )

                model = AutoModelForSequenceClassification.from_pretrained(
                    self.model_config.model_name,
                    cache_dir=self.cache_dir,
                    return_dict=True
                )

                model.to(self.device)
                model.eval()

                self.__class__._tokenizer = tokenizer
                self.__class__._model = model

                self._loaded = True

                logger.info("Sentiment model loaded successfully")

            except Exception as e:
                logger.error("Sentiment model loading failed", exc_info=True)
                raise RuntimeError(str(e))

    # =========================================================
    # Prediction
    # =========================================================

    def predict(
        self,
        text: str,
        return_proba: bool = False
    ) -> Dict[str, Any]:

        if not self._loaded:
            self.load()

        with self._predict_lock:

            try:
                inputs = self._tokenizer(
                    text,
                    max_length=self.inference_config.max_length,
                    padding=True,
                    truncation=True,
                    return_tensors="pt"
                ).to(self.device)

                with torch.inference_mode():
                    outputs = self._model(**inputs)

                probs = torch.nn.functional.softmax(
                    outputs.logits,
                    dim=1
                )

                predicted_class = torch.argmax(probs, dim=1).item()

                label_map = self._model.config.id2label
                label = label_map[predicted_class]

                result = {
                    "label": label,
                    "class_id": predicted_class
                }

                if return_proba:
                    probabilities = {
                        label_map[i]: float(probs[0][i])
                        for i in range(len(label_map))
                    }
                    result["sentiment_confidence"] = round(max(probabilities.values()), 2)

                return result

            except torch.cuda.OutOfMemoryError:
                torch.cuda.empty_cache()
                raise RuntimeError("Недостаточно VRAM")

            except Exception as e:
                logger.error("Sentiment prediction error", exc_info=True)
                raise RuntimeError(str(e))

    # =========================================================
    # Health + Lifecycle
    # =========================================================

    def health_check(self) -> Dict[str, Any]:

        info = dict(
            model=self.model_config.model_name,
            device=self.device,
            loaded=self._loaded
        )

        if torch.cuda.is_available():
            info["gpu"] = torch.cuda.get_device_name(0)
            info["free_vram_gb"] = round(
                torch.cuda.mem_get_info()[0] / 1024**3,
                2
            )

        return info

    def unload(self):

        if self.__class__._model is not None:
            del self.__class__._model

        if self.__class__._tokenizer is not None:
            del self.__class__._tokenizer

        self.__class__._model = None
        self.__class__._tokenizer = None

        if torch.cuda.is_available():
            torch.cuda.empty_cache()

        self._loaded = False

        logger.info("Sentiment resources released")

    # Context manager

    def __enter__(self):
        self.load()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.unload()
        return False