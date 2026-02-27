from __future__ import annotations
import uuid
from typing import Iterable, List

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

    def _make_chunk_id(self, doc_id: str, chunk_index: int) -> str:
        """Generate deterministic UUID for chunk."""
        return str(uuid.uuid5(
            uuid.NAMESPACE_URL,
            f"{doc_id}_{chunk_index}"
        ))

    def _chunk_text(self, doc: Document) -> List[Chunk]:
        words = doc.text.split()
        total = len(words)
        i = 0

        chunk_list: List[Chunk] = []
        chunk_index = 0

        while i < total:
            end = min(i + self.chunk_size, total)

            chunk_words = words[i:end]
            chunk_text = " ".join(chunk_words)

            # FIX: valid UUID for Qdrant
            chunk_id = self._make_chunk_id(doc.id, chunk_index)

            metadata = {**doc.metadata}
            metadata.update({
                "source_document_id": doc.id,
                "chunk_index": chunk_index
            })

            chunk = Chunk(
                id=chunk_id,
                document_id=doc.id,
                text=chunk_text,
                metadata=metadata
            )

            chunk_list.append(chunk)
            chunk_index += 1

            if end == total:
                break

            i = end - self.chunk_overlap

            if i < 0:
                i = 0

        logger.debug(f"Generated {len(chunk_list)} chunks for document {doc.id}")
        return chunk_list