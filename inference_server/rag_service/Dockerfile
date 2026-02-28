FROM python:3.13-slim

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential git \
    && rm -rf /var/lib/apt/lists/*

RUN pip install \
    fastapi uvicorn[standard] \
    qdrant-client \
    pydantic-settings loguru \
    onnxruntime \
    tokenizers \
    numpy

COPY rag_system ./rag_system
COPY inference_server/rag_service ./inference_server/rag_service
COPY all-MiniLM-L6-v2 ./all-MiniLM-L6-v2

EXPOSE 8002

CMD ["uvicorn", "inference_server.rag_service.app.main:app", "--host", "0.0.0.0", "--port", "8002", "--loop", "uvloop", "--workers", "1"]
