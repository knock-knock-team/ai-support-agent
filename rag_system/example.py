"""Пример использования компонентов RAG backend.

1. Создаёт коллекцию "test" в Qdrant.
2. Читает два текста (Пушкин) и индексирует их как документы.
3. Позволяет отправлять произвольный текст и получать похожие куски из базы.

Запуск:
    python example.py

Перед использованием убедитесь, что в окружении настроены переменные
QDRANT_URL (и при необходимости QDRANT_API_KEY).
"""

import os
import sys
from typing import List

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

try:
    from rag_system.models.schemas import Document
    from rag_system.preprocessing.chunker import Chunker
    from rag_system.embeddings.sentence_transformer_embedder import SentenceTransformerEmbedder
    from rag_system.vectorstore.qdrant_store import QdrantVectorStore
    from rag_system.indexing.indexer import Indexer
    from rag_system.retrieval.retriever import Retriever
    from rag_system.utils.logging import logger
    from rag_system.config import settings
except ImportError as exc:
    print("Ошибка импорта модуля rag_system или зависимостей. Убедитесь, что вы используете виртуальную среду, где установлены требуемые пакеты.")
    print("Сообщение об ошибке:", exc)
    sys.exit(1)

# end of guard imports

BOOK_PATHS = [
    r"C:\Users\333\ai-support-agent\rag_system\Pushkin Aleksandr. Evgeniy Onegin - BooksCafe.Net.txt",
    r"C:\Users\333\ai-support-agent\rag_system\Pushkin Aleksandr. Kapitanskaya dochka - BooksCafe.Net.txt",
]


def read_document(path: str) -> Document:
    """Прочитать файл и вернуть Document с именем файла как id."""
    try:
        with open(path, "r", encoding="utf-8") as f:
            text = f.read()
    except Exception as e:
        logger.error(f"Не удалось прочитать файл {path}: {e}")
        raise
    doc_id = os.path.splitext(os.path.basename(path))[0]
    return Document(id=doc_id, text=text, metadata={"source": doc_id})


def build_index(collection_name: str = "test") -> (SentenceTransformerEmbedder, QdrantVectorStore):
    """Создаёт векторное хранилище и индексирует книги.

    Возвращает инстансы embedder и store, чтобы их можно было
    использовать для запросов дальше.
    """
    embedder = SentenceTransformerEmbedder()
    store = QdrantVectorStore(collection_name=collection_name)
    chunker = Chunker()
    indexer = Indexer(embedder=embedder, vector_store=store, chunker=chunker)

    docs: List[Document] = []
    for path in BOOK_PATHS:
        if os.path.exists(path):
            docs.append(read_document(path))
        else:
            logger.warning(f"Файл не найден: {path}")
    if docs:
        logger.info(f"Индексируем {len(docs)} документов")
        indexer.index_documents(docs)
    else:
        logger.warning("Нет документов для индексации")
    return embedder, store


def interactive_loop(embedder: SentenceTransformerEmbedder, store: QdrantVectorStore):
    retriever = Retriever(embedder=embedder, vector_store=store)
    print("Готово. Введите текст для поиска, пустая строка — выход.")
    try:
        while True:
            query = input("> ").strip()
            if not query:
                break
            results = retriever.retrieve(query, top_k=5)
            if not results:
                print("Ничего не найдено.")
                continue
            for r in results:
                print("score=%.4f source=%s chunk='%.200s...'" % (
                    r.score,
                    r.chunk.metadata.get("source", ""),
                    r.chunk.text.replace("\n", " ")
                ))
    except KeyboardInterrupt:
        print("Прекращено пользователем")


if __name__ == "__main__":
    if not settings.qdrant_url:
        print("Не задана переменная окружения QDRANT_URL, пример не будет работать.")
        sys.exit(1)

    emb, st = build_index(collection_name="test")
    interactive_loop(emb, st)
