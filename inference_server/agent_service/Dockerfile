FROM python:3.13-slim

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY inference_server/agent_service/requirements.txt requirements.txt
RUN pip install -r requirements.txt 
    
COPY agent/ ./agent/
COPY inference_server/agent_service ./inference_server/agent_service

EXPOSE 8001

CMD ["uvicorn", "inference_server.agent_service.app.main:app", "--host", "0.0.0.0", "--port", "8001", "--loop", "uvloop", "--workers", "1"]
