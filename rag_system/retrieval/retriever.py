from __future__ import annotations
from typing import Any, Dict, List, Optional

from rag_system.embeddings.base import Embedder
from rag_system.vectorstore.base import VectorStore
from rag_system.models.schemas import RetrievalResult, Chunk
from rag_system.utils.logging import logger


class Retriever:
    def __init__(
        self,
        embedder: Embedder,
        vector_store: VectorStore,
        reranker: Any | None = None,
    ):
        self.embedder = embedder
        self.vector_store = vector_store
        self.reranker = reranker

    def retrieve(
        self,
        query: str,
        top_k: int = 5,
        filters: Optional[Dict[str, Any]] = None,
    ) -> List[RetrievalResult]:
        logger.debug(f"Retrieving top_k={top_k} for query='{query}'")
        query_vec = self.embedder.embed([query])[0]
        hits = self.vector_store.search(query_vector=query_vec, top_k=top_k, filter=filters)
        results: List[RetrievalResult] = []
        for hit in hits:
            metadata = hit.get("metadata", {})
            chunk = Chunk(
                id=hit["id"],
                document_id=metadata.get("source_document_id", ""),
                text=metadata.get("text", ""),
                metadata=metadata,
            )
            rr = RetrievalResult(chunk=chunk, score=hit["score"])
            results.append(rr)

        # placeholder reranking
        if self.reranker:
            logger.debug("Calling reranker (stub)")
            # suppose reranker modifies results in place or returns new list

        return results
