import argparse
import json
import sys
from typing import List

from rag_system.config import settings
from rag_system.embeddings.sentence_transformer_embedder import SentenceTransformerEmbedder
from rag_system.indexing.indexer import Indexer
from rag_system.preprocessing.chunker import Chunker
from rag_system.retrieval.retriever import Retriever
from rag_system.vectorstore.qdrant_store import QdrantVectorStore
from rag_system.models.schemas import Document
from rag_system.utils.logging import logger


def index_documents(input_file: str) -> None:
    """Read a JSON lines or list of documents and index them."""
    try:
        with open(input_file, "r", encoding="utf-8") as f:
            data = json.load(f)
    except json.JSONDecodeError:
        # try line-by-line
        docs: List[Document] = []
        with open(input_file, "r", encoding="utf-8") as f:
            for line in f:
                if line.strip():
                    docs.append(Document.parse_raw(line))
    else:
        if isinstance(data, list):
            docs = [Document.parse_obj(d) for d in data]
        else:
            docs = [Document.parse_obj(data)]
    logger.info(f"Indexing {len(docs)} documents")
    embedder = SentenceTransformerEmbedder()
    vector_store = QdrantVectorStore()
    indexer = Indexer(embedder=embedder, vector_store=vector_store, chunker=Chunker())
    indexer.index_documents(docs)


def query_loop(top_k: int):
    embedder = SentenceTransformerEmbedder()
    vector_store = QdrantVectorStore()
    retriever = Retriever(embedder=embedder, vector_store=vector_store)
    print("enter query (ctrl-c to exit)")
    try:
        while True:
            q = input("> ")
            if not q:
                continue
            results = retriever.retrieve(q, top_k=top_k)
            for r in results:
                print(f"score={r.score:.4f} text={r.chunk.text[:200]}")
    except KeyboardInterrupt:
        print("bye")


def main() -> None:
    parser = argparse.ArgumentParser(description="RAG backend sample CLI")
    sub = parser.add_subparsers(dest="cmd")
    idx = sub.add_parser("index", help="index documents from file")
    idx.add_argument("file", help="input JSON or JSONL file")
    qry = sub.add_parser("query", help="enter interactive query mode")
    qry.add_argument("-k", type=int, default=settings.default_top_k, help="top k results")
    args = parser.parse_args()
    if args.cmd == "index":
        index_documents(args.file)
    elif args.cmd == "query":
        query_loop(args.k)
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
