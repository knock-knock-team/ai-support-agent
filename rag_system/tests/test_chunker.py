from rag_system.preprocessing.chunker import Chunker
from rag_system.models.schemas import Document


def test_chunker_basic():
    text = "".join([f"word{i} " for i in range(100)])
    doc = Document(id="doc1", text=text)
    chunker = Chunker(chunk_size=10, chunk_overlap=2)
    chunks = chunker.chunk_documents([doc])
    assert len(chunks) > 0
    # ensure overlap
    assert chunks[0].text.split()[-2:] == chunks[1].text.split()[:2]


def test_chunker_metadata():
    doc = Document(id="d2", text="one two three four five")
    chunker = Chunker(chunk_size=2, chunk_overlap=1)
    chunks = chunker.chunk_documents([doc])
    assert all(ch.document_id == "d2" for ch in chunks)
    assert all("source_document_id" in ch.metadata for ch in chunks)
    assert all("chunk_index" in ch.metadata for ch in chunks)
