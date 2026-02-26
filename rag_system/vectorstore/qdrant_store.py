from __future__ import annotations
from typing import Any, Dict, List, Optional

from qdrant_client import QdrantClient
from qdrant_client.http import models
from qdrant_client.http.models import Filter, FieldCondition, MatchValue

from rag_system.vectorstore.base import VectorStore
from rag_system.config import settings
from rag_system.utils.logging import logger


class QdrantVectorStore(VectorStore):
    def __init__(self, collection_name: str = "rag_collection"):
        self.collection_name = collection_name
        self.client = QdrantClient(
            url=settings.qdrant_url, api_key=settings.qdrant_api_key
        )
        # ensure collection exists (simple schema)
        logger.info(f"Initializing Qdrant collection '{self.collection_name}'")
        # ensure collection exists; do not wipe data on each start
        try:
            self.client.get_collection(self.collection_name)
            logger.debug("Collection already exists, skipping creation")
        except Exception:
            # if not found, create with a generic vector config
            # BAAI/bge-small-en-v1.5 produces 384-dimensional vectors
            self.client.create_collection(
                collection_name=self.collection_name,
                vectors_config=models.VectorParams(size=384, distance=models.Distance.COSINE),
            )

    def upsert(
        self,
        vectors: List[List[float]],
        ids: List[str],
        metadatas: Optional[List[Dict[str, Any]]] = None,
    ) -> None:
        payload = []
        for idx, vec in enumerate(vectors):
            item = {"id": ids[idx], "vector": vec}
            if metadatas:
                item["payload"] = metadatas[idx]
            payload.append(item)
        logger.debug(f"Upserting {len(payload)} vectors into Qdrant")
        self.client.upsert(
            collection_name=self.collection_name, points=payload
        )

    def search(
        self,
        query_vector: List[float],
        top_k: int = 5,
        filter: Optional[Dict[str, Any]] = None,
    ) -> List[Dict[str, Any]]:
        qdrant_filter = self._build_filter(filter) if filter else None
        logger.debug(
            f"Searching in Qdrant top_k={top_k}, filter={filter}"
        )
        result = self.client.search(
            collection_name=self.collection_name,
            query_vector=query_vector,
            limit=top_k,
            filter=qdrant_filter,
        )
        output: List[Dict[str, Any]] = []
        for hit in result:
            output.append(
                {
                    "id": hit.id,
                    "score": hit.score,
                    "metadata": hit.payload,
                }
            )
        return output

    def _build_filter(self, criteria: Dict[str, Any]) -> Filter:
        # simple AND of equality conditions
        must = []
        for key, value in criteria.items():
            must.append(
                FieldCondition(
                    key=key,
                    match=MatchValue(value=value),
                )
            )
        return Filter(must=must)
