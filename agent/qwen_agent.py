import os
import logging
import threading
import torch

from dataclasses import dataclass
from typing import Optional, Dict, Any

from dotenv import load_dotenv
import torch.nn.functional as F
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
            temperature=0.2,
            top_p=1.0,
            repetition_penalty=1.1
        )

    @classmethod
    def classification_default(cls):
        return cls(
            max_new_tokens=10,
            temperature=0.01,
            top_p=1.0,
            repetition_penalty=1.0
        )
    
    @classmethod
    def extraction_default(cls):
        return cls(
            max_new_tokens=256,
            temperature=0.01,
            top_p=1.0,
            repetition_penalty=1.0
        )
    
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
        self.service_config = service_config or ServiceConfig()

        self._loaded = False

    # =========================================================
    # Utilities
    # =========================================================

    def _is_valid_json(self, text: str) -> bool:
        try:
            json.loads(text)
            return True
        except ValueError:
            return False
    
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
    
    @staticmethod
    def compute_confidence(inputs, output):

        log_probs = []

        prompt_len = inputs.input_ids.shape[1]

        for step, step_scores in enumerate(output.scores):
            probs = F.log_softmax(step_scores, dim=-1)

            token_id = output.sequences[0][prompt_len + step]

            log_prob = probs[0, token_id]
            log_probs.append(log_prob)

        if not log_probs:
            return 0.0

        confidence = round(torch.exp(torch.mean(torch.stack(log_probs))).item(), 2)

        return confidence
    
    def extraction_structure_score(self, response):

        if not self._is_valid_json(response):
            return None

        try:
            data = json.loads(response)
        except:
            return 0.0

        total_fields = len(data)

        if total_fields == 0:
            return 0.0

        filled = sum(
            1 for v in data.values()
            if v not in [None, "", [], {}, "null"]
        )

        return filled / total_fields

    def compute_extraction_confidence(self, inputs, output, response):

        structure_score = self.extraction_structure_score(response)

        generation_score = self.compute_confidence(inputs, output)

        confidence = (
            0.5 * structure_score +
            0.5 * generation_score
        )

        return round(confidence, 2)
    
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
    # Prompt Builder
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
            "Ты — система извлечения информации из обращения пользователя.\n"
            "\n"
            "Извлеки следующие поля из текста обращения:\n"
            "\n"
            "1. date — дата поступления письма\n"
            "2. fio — фамилия, имя, отчество отправителя\n"
            "3. organization — название предприятия или объекта\n"
            "4. phone — контактный номер телефона\n"
            "5. email — адрес электронной почты\n"
            "6. serial_numbers — номера приборов (список строк)\n"
            "7. device_type — тип или модель приборов (список строк)\n"
            "8. problem_summary — краткое описание сути обращения\n"
            "\n"
            "Правила:\n"
            "- Ответь только в формате JSON.\n"
            "- Не добавляй поясняющий текст.\n"
            "- Если информации недостаточно — оставь в поле null.\n"
            "- problem_summary должен быть не длиннее 1 предложения.\n"
            "- В problem_summary не указывай личные данные. Указывай только суть обращения."
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
                    output = self._model.generate(
                        **inputs,
                        max_new_tokens=config.max_new_tokens,
                        temperature=config.temperature,
                        top_p=config.top_p,
                        repetition_penalty=config.repetition_penalty,
                        pad_token_id=self._tokenizer.pad_token_id,
                        eos_token_id=self._tokenizer.eos_token_id,
                        output_scores=True,
                        return_dict_in_generate=True
                    )

                generated = output.sequences[0][len(inputs.input_ids[0]):]

                response = self._tokenizer.decode(
                    generated,
                    skip_special_tokens=True
                ).strip()

                confidence = None

                if mode == "qa" or mode == "classification":
                    confidence = self.compute_confidence(inputs, output)

                elif mode == "extraction":
                    confidence = self.compute_extraction_confidence(inputs, output, response)
                    if not confidence:
                        return None
                
                return {
                    "response": response,
                    "confidence": confidence
                }

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