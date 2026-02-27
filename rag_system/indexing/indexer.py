from __future__ import annotations
from typing import Iterable, List

from rag_system.embeddings.base import Embedder
from rag_system.models.schemas import Document, Chunk
from rag_system.preprocessing.chunker import Chunker
from rag_system.vectorstore.base import VectorStore
from rag_system.utils.logging import logger


class Indexer:
    def __init__(
        self,
        embedder: Embedder,
        vector_store: VectorStore,
        chunker: Chunker | None = None,
    ):
        self.embedder = embedder
        self.vector_store = vector_store
        self.chunker = chunker or Chunker()

    def index_documents(self, docs: Iterable[Document]) -> None:
        # chunk
        chunks: List[Chunk] = self.chunker.chunk_documents(docs)
        if not chunks:
            logger.warning("No chunks produced, skipping indexing")
            return
        texts = [c.text for c in chunks]
        ids = [c.id for c in chunks]
        # embed metadata copy with text included
        metadatas = [{**c.metadata, "text": c.text} for c in chunks]
        # embed
        vectors = self.embedder.embed(texts)
        # upsert
        self.vector_store.upsert(vectors=vectors, ids=ids, metadatas=metadatas)
        logger.info(f"Indexed {len(chunks)} chunks")
