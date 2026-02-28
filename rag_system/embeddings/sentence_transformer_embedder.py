from __future__ import annotations
from typing import List

import numpy as np
import onnxruntime as ort
from tokenizers import Tokenizer

from .base import Embedder
from ..config import settings
from ..utils.logging import logger


class SentenceTransformerEmbedder(Embedder):
    def __init__(self, model_name: str | None = None, tokenizer_name: str | None = None):
        self.model_name = model_name or settings.embedding_model_onnx
        self.tokenizer_name = tokenizer_name or settings.embedding_model_onnx_tokenizer

        logger.info(
            f"Loading ONNX embedding model {self.model_name} and tokenizer {self.tokenizer_name}"
        )

        self.tokenizer = Tokenizer.from_file(self.tokenizer_name)
        
        self.use_cuda = ort.get_device() == "GPU"

        providers = (
            ["CUDAExecutionProvider", "CPUExecutionProvider"]
            if self.use_cuda
            else ["CPUExecutionProvider"]
        )

        logger.info(f"ONNX providers: {providers}")

        self.session = ort.InferenceSession(
            self.model_name,
            providers=providers,
        )

        # cache input/output names for speed
        self.input_names = [inp.name for inp in self.session.get_inputs()]
        self.output_name = self.session.get_outputs()[0].name

    def _tokenize(self, texts: List[str]):
        encodings = [self.tokenizer.encode(t) for t in texts]

        max_len = max(len(enc.ids) for enc in encodings)

        input_ids = np.zeros((len(texts), max_len), dtype=np.int64)
        attention_mask = np.zeros((len(texts), max_len), dtype=np.int64)

        token_type_ids = np.zeros((len(texts), max_len), dtype=np.int64)

        for i, enc in enumerate(encodings):
            ids = enc.ids
            input_ids[i, : len(ids)] = ids
            attention_mask[i, : len(ids)] = 1

        return input_ids, attention_mask, token_type_ids

    def embed(self, texts: List[str]) -> List[List[float]]:
        logger.debug(f"Embedding {len(texts)} texts using ONNX")

        input_ids, attention_mask, token_type_ids = self._tokenize(texts)

        ort_inputs = {}

        if "input_ids" in self.input_names:
            ort_inputs["input_ids"] = input_ids

        if "attention_mask" in self.input_names:
            ort_inputs["attention_mask"] = attention_mask
       
        if "token_type_ids" in self.input_names:
            ort_inputs["token_type_ids"] = token_type_ids

        outputs = self.session.run([self.output_name], ort_inputs)

        last_hidden_state = outputs[0]

        mask = attention_mask[..., None]
        sum_hidden = (last_hidden_state * mask).sum(axis=1)
        lengths = np.clip(mask.sum(axis=1), a_min=1, a_max=None)

        embeddings = sum_hidden / lengths

        return embeddings.tolist()