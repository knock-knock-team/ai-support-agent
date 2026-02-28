import os
import logging
import threading
import torch

from dataclasses import dataclass
from typing import Optional, Dict, Any

from dotenv import load_dotenv
from transformers import (
    AutoTokenizer,
    AutoModelForCausalLM,
    BitsAndBytesConfig
)

import json
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
    model_name: str = "Qwen/Qwen2.5-3B-Instruct"
    use_4bit: bool = True

@dataclass
class ServiceConfig:
    context_max_chars: int = 4000

@dataclass
class InferenceConfig:
    max_new_tokens: int
    temperature: float
    top_p: float
    repetition_penalty: float

    @classmethod
    def qa_default(cls):
        return cls(
            max_new_tokens=512,
            temperature=0.6,
            top_p=0.9,
            repetition_penalty=1.1
        )

    @classmethod
    def classification_default(cls):
        return cls(
            max_new_tokens=10,
            temperature=0.1,
            top_p=1.0,
            repetition_penalty=1.0
        )
    
    @classmethod
    def extraction_default(cls):
        return cls(
            max_new_tokens=256,
            temperature=0.1,
            top_p=1.0,
            repetition_penalty=1.0
        )

def is_valid_json(text: str) -> bool:
    try:
        json.loads(text)
        return True
    except ValueError:
        return False
    
class AgentGenerationProfiles:
    QA = InferenceConfig.qa_default()
    CLASSIFICATION = InferenceConfig.classification_default()
    EXTRACTION = InferenceConfig.extraction_default()

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
        # inference_config: Optional[InferenceConfig] = None,
        service_config: Optional[ServiceConfig] = None
    ):
        load_dotenv()

        self.cache_dir = cache_dir or os.path.join(
            os.path.dirname(os.path.abspath(__file__)),
            "models_cache"
        )

        self.device = device or (
            "cuda" if torch.cuda.is_available() else "cpu"
        )

        self.model_config = model_config or ModelConfig()
        # self.inference_config = inference_config or InferenceConfig()
        self.service_config = service_config or ServiceConfig()

        self._loaded = False

    # =========================================================
    # Utilities
    # =========================================================

    def _truncate_context(self, text: str) -> str:
        if not text:
            return ""

        return text[: self.service_config.context_max_chars]

    def _get_quant_config(self):
        if not self.model_config.use_4bit:
            return None

        if self.device == "cpu":
            return None

        return BitsAndBytesConfig(
            load_in_4bit=True,
            bnb_4bit_quant_type="nf4",
            bnb_4bit_compute_dtype=torch.bfloat16,
            bnb_4bit_use_double_quant=True,
        )

    # =========================================================
    # Model Lifecycle
    # =========================================================

    def load(self):

        if self._loaded:
            return

        with self._load_lock:

            if self._loaded:
                return

            logger.info(f"Loading model {self.model_config.model_name}")

            os.makedirs(self.cache_dir, exist_ok=True)

            try:
                tokenizer = AutoTokenizer.from_pretrained(
                    self.model_config.model_name,
                    cache_dir=self.cache_dir,
                    trust_remote_code=True,
                    use_fast=True
                )

                if tokenizer.pad_token is None:
                    tokenizer.pad_token = tokenizer.eos_token

                quant_config = self._get_quant_config()

                model_kwargs = dict(
                    pretrained_model_name_or_path=self.model_config.model_name,
                    cache_dir=self.cache_dir,
                    trust_remote_code=True,
                    low_cpu_mem_usage=True
                )

                if self.device == "cuda":
                    model_kwargs["device_map"] = "auto"

                if quant_config:
                    logger.info("Using 4-bit quantization")
                    model_kwargs["quantization_config"] = quant_config
                else:
                    model_kwargs["torch_dtype"] = "auto"

                model = AutoModelForCausalLM.from_pretrained(**model_kwargs)

                self.__class__._model = model
                self.__class__._tokenizer = tokenizer

                self._loaded = True

                logger.info("Model loaded successfully")

            except Exception as e:
                logger.error("Model loading failed", exc_info=True)
                raise RuntimeError(str(e))

    # =========================================================
    # Prompt Builder (Russian prompts)
    # =========================================================

    def _build_prompt(
        self,
        question: str,
        context: Optional[str] = None,
        system_prompt: Optional[str] = None,
        mode: str = "qa"
    ) -> str:

        if mode == "qa":
            return self._build_qa_prompt(question, context, system_prompt)

        elif mode == "classification":
            return self._build_classification_prompt(question, system_prompt)

        elif mode == "extraction":
            return self._build_extraction_prompt(question, system_prompt)
        
        else:
            raise ValueError(f"Unknown mode {mode}")

    def _build_qa_prompt(self, question, context=None, system_prompt=None):

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

    def _build_classification_prompt(self, question, system_prompt=None):

        system_prompt = system_prompt or (
        "Ты — ассистент службы поддержки. "
        "Тебе предоставлено сообщение от пользователя. "
        "Твоя задача верно классифицировать сообщение. "
        "Всего есть следующие категории: "
        "\"Калибровка оборудования\", "
        "\"Неисправность\", "
        "\"Запрос документации\", "
        "\"Другая категория\". "
        "В качестве ответа предоставь только имя категории. "
        "Других категорий не существует."
        )

        messages = [
            {"role": "system", "content": system_prompt},
            {
                "role": "user",
                "content": f"Сообщение пользователя:\n{question}"
            }
        ]

        return self._tokenizer.apply_chat_template(
            messages,
            tokenize=False,
            add_generation_prompt=True
        )

    def _build_extraction_prompt(self, question, system_prompt=None):

        system_prompt = system_prompt or (
            """Ты — система извлечения информации из обращения пользователя.

        Извлеки следующие поля из текста обращения:

        1. date — дата поступления письма
        2. fio — фамилия, имя, отчество отправителя
        3. object — название предприятия или объекта
        4. phone — контактный номер телефона
        5. email — адрес электронной почты
        6. serial_numbers — номера приборов (список строк)
        7. device_types — тип или модель приборов (список строк)
        8. problem_summary — краткое описание сути обращения

        Правила:
        - Ответь только в формате JSON.
        - Не добавляй поясняющий текст.
        - Если информации недостаточно — оставь в поле null.
        - problem_summary должен быть не длиннее 1 предложения."""
        )

        messages = [
            {"role": "system", "content": system_prompt},
            {
                "role": "user",
                "content": f"Сообщение пользователя:\n{question}"
            }
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
        system_prompt: Optional[str] = None,
        mode: str = "qa"
    ) -> str:

        if not self._loaded:
            self.load()

        if config is None:
            if mode == "qa":
                config = AgentGenerationProfiles.QA
            elif mode == "classification":
                config = AgentGenerationProfiles.CLASSIFICATION
            elif mode == "extraction":
                config = AgentGenerationProfiles.EXTRACTION
            else:
                raise ValueError(f"Unknown mode {mode}")

        with self._generate_lock:

            try:
                prompt = self._build_prompt(
                    question,
                    context,
                    system_prompt,
                    mode
                )

                inputs = self._tokenizer(
                    [prompt],
                    return_tensors="pt"
                ).to(self._model.device)

                with torch.inference_mode():
                    output_ids = self._model.generate(
                        **inputs,
                        max_new_tokens=config.max_new_tokens,
                        temperature=config.temperature,
                        top_p=config.top_p,
                        repetition_penalty=config.repetition_penalty,
                        pad_token_id=self._tokenizer.pad_token_id,
                        eos_token_id=self._tokenizer.eos_token_id,
                    )

                generated = output_ids[0][len(inputs.input_ids[0]):]

                response = self._tokenizer.decode(
                    generated,
                    skip_special_tokens=True
                ).strip()

                if mode == "extraction" and not is_valid_json(response):
                    return None
                return response

            except torch.cuda.OutOfMemoryError:
                torch.cuda.empty_cache()
                raise RuntimeError("Недостаточно VRAM")

            except Exception as e:
                logger.error("Generation error", exc_info=True)
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

        logger.info("Resources released")

    # Context manager

    def __enter__(self):
        self.load()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.unload()
        return False