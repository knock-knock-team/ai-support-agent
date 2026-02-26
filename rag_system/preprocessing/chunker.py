from __future__ import annotations
import uuid
from typing import Any, Dict, Iterable, List

from rag_system.models.schemas import Chunk, Document
from rag_system.config import settings
from rag_system.utils.logging import logger


class Chunker:
    """Splits documents into token-aware chunks while preserving metadata."""

    def __init__(self, chunk_size: int | None = None, chunk_overlap: int | None = None):
        self.chunk_size = chunk_size or settings.chunk_size
        self.chunk_overlap = chunk_overlap or settings.chunk_overlap

    def chunk_documents(self, docs: Iterable[Document]) -> List[Chunk]:
        chunks: List[Chunk] = []
        for doc in docs:
            logger.debug(f"Chunking document {doc.id}")
            doc_chunks = self._chunk_text(doc)
            chunks.extend(doc_chunks)
        return chunks

    def _chunk_text(self, doc: Document) -> List[Chunk]:
        words = doc.text.split()  # simple tokenization
        total = len(words)
        i = 0
        chunk_list: List[Chunk] = []
        while i < total:
            end = min(i + self.chunk_size, total)
            chunk_words = words[i:end]
            chunk_text = " ".join(chunk_words)
            # deterministic id based on document id and chunk index
            chunk_id = f"{doc.id}-{len(chunk_list)}"
            metadata = {**doc.metadata}
            metadata.update({"source_document_id": doc.id, "chunk_index": len(chunk_list)})
            chunk = Chunk(id=chunk_id, document_id=doc.id, text=chunk_text, metadata=metadata)
            chunk_list.append(chunk)

            if end == total:
                break
            i = end - self.chunk_overlap
            # avoid infinite loop
            if i < 0:
                i = 0
        logger.debug(f"Generated {len(chunk_list)} chunks for document {doc.id}")
        return chunk_list
