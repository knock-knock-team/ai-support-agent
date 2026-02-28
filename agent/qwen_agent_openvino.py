import os
import logging
import threading
import torch

from dataclasses import dataclass
from typing import Optional, Dict, Any

from dotenv import load_dotenv
from transformers import AutoTokenizer

from optimum.intel.openvino import OVModelForCausalLM


# =========================================================
# Logging
# =========================================================

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)s | %(message)s"
)

logger = logging.getLogger("Agent")


# =========================================================
# Configs
# =========================================================

@dataclass
class ModelConfig:
    model_name: str = "openvino_model"
    use_4bit: bool = False  # OpenVINO does not use bitsandbytes


@dataclass
class InferenceConfig:
    max_new_tokens: int = 512
    temperature: float = 0.7
    top_p: float = 0.8
    repetition_penalty: float = 1.1


@dataclass
class ServiceConfig:
    context_max_chars: int = 4000


# =========================================================
# Agent
# =========================================================

class QwenAgent:
    _model = None
    _tokenizer = None
    _load_lock = threading.Lock()
    _generate_lock = threading.Lock()

    def __init__(
        self,
        cache_dir: Optional[str] = None,
        device: Optional[str] = None,
        model_config: Optional[ModelConfig] = None,
        inference_config: Optional[InferenceConfig] = None,
        service_config: Optional[ServiceConfig] = None,
    ):
        load_dotenv()

        base_dir = os.path.dirname(os.path.abspath(__file__))

        self.cache_dir = cache_dir or os.path.join(base_dir, "models_cache")

        # OpenVINO device naming
        if device:
            self.device = device
        else:
            self.device = "GPU" if torch.cuda.is_available() else "CPU"

        self.model_config = model_config or ModelConfig()
        self.inference_config = inference_config or InferenceConfig()
        self.service_config = service_config or ServiceConfig()

        # force local path
        if not os.path.isabs(self.model_config.model_name):
            self.model_path = os.path.join(base_dir, self.model_config.model_name)
        else:
            self.model_path = self.model_config.model_name

        self._loaded = False

    # =========================================================
    # Utilities
    # =========================================================

    def _truncate_context(self, text: str) -> str:
        if not text:
            return ""
        return text[: self.service_config.context_max_chars]

    def _get_quant_config(self):
        # Not used in OpenVINO
        return None

    # =========================================================
    # Model Lifecycle
    # =========================================================

    def load(self):

        if self._loaded:
            return

        with self._load_lock:

            if self._loaded:
                return

            logger.info(f"Loading OpenVINO model from {self.model_path}")
            logger.info(f"Device: {self.device}")

            try:

                tokenizer = AutoTokenizer.from_pretrained(
                    self.model_path,
                    trust_remote_code=True
                )

                if tokenizer.pad_token is None:
                    tokenizer.pad_token = tokenizer.eos_token

                model = OVModelForCausalLM.from_pretrained(
                    self.model_path,
                    device=self.device,
                    trust_remote_code=True,
                    compile=True
                )

                self.__class__._model = model
                self.__class__._tokenizer = tokenizer

                self._loaded = True

                logger.info("OpenVINO model loaded successfully")

            except Exception as e:
                logger.error("Model loading failed", exc_info=True)
                raise RuntimeError(str(e))

    # =========================================================
    # Prompt Builder
    # =========================================================

    def _build_prompt(
        self,
        question: str,
        context: Optional[str] = None,
        system_prompt: Optional[str] = None
    ) -> str:

        system_prompt = system_prompt or (
            "Ты — ассистент службы поддержки. "
            "Отвечай только на основе предоставленного контекста. "
            "Если ответа нет в контексте — скажи, что информации недостаточно."
        )

        user_content = ""

        if context:
            context = self._truncate_context(context)
            user_content += f"Контекст:\n{context}\n\n"

        user_content += f"Вопрос:\n{question}\n\n"
        user_content += "Ответь строго используя предоставленный контекст."

        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_content}
        ]

        return self._tokenizer.apply_chat_template(
            messages,
            tokenize=False,
            add_generation_prompt=True
        )

    # =========================================================
    # Generation
    # =========================================================

    def generate(
        self,
        question: str,
        context: Optional[str] = None,
        config: Optional[InferenceConfig] = None,
        system_prompt: Optional[str] = None
    ) -> str:

        if not self._loaded:
            self.load()

        config = config or self.inference_config

        with self._generate_lock:

            try:

                prompt = self._build_prompt(
                    question,
                    context,
                    system_prompt
                )

                inputs = self._tokenizer(
                    prompt,
                    return_tensors="pt"
                )

                output_ids = self._model.generate(
                    **inputs,
                    max_new_tokens=config.max_new_tokens,
                    temperature=config.temperature,
                    top_p=config.top_p,
                    repetition_penalty=config.repetition_penalty,
                    pad_token_id=self._tokenizer.pad_token_id,
                    eos_token_id=self._tokenizer.eos_token_id,
                )

                generated = output_ids[0][inputs.input_ids.shape[1]:]

                return self._tokenizer.decode(
                    generated,
                    skip_special_tokens=True
                ).strip()

            except Exception as e:
                logger.error("Generation error", exc_info=True)
                raise RuntimeError(str(e))

    # =========================================================
    # Health + Lifecycle
    # =========================================================

    def health_check(self) -> Dict[str, Any]:

        return dict(
            model=self.model_path,
            device=self.device,
            loaded=self._loaded,
            backend="openvino"
        )

    def unload(self):

        if self.__class__._model is not None:
            del self.__class__._model

        if self.__class__._tokenizer is not None:
            del self.__class__._tokenizer

        self.__class__._model = None
        self.__class__._tokenizer = None

        self._loaded = False

        logger.info("OpenVINO resources released")

    # =========================================================
    # Context manager
    # =========================================================

    def __enter__(self):
        self.load()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.unload()
        return False