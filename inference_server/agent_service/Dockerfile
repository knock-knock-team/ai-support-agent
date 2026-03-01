FROM python:3.13-slim

# set up working directory
WORKDIR /app

# install system dependencies
RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential git \
    && rm -rf /var/lib/apt/lists/*

# copy and install python dependencies
COPY agent/requirements.txt ./agent_requirements.txt
RUN pip install --no-cache-dir -r agent_requirements.txt \ 
    fastapi uvicorn[standard] pydantic[dotenv] starlette torch
# copy project code (agent and service modules)
COPY agent/ ./agent/
COPY inference_server/agent_service ./inference_server/agent_service

# expose port
EXPOSE 8001

# use uvicorn to run
CMD ["uvicorn", "inference_server.agent_service.app.main:app", "--host", "0.0.0.0", "--port", "8001", "--loop", "uvloop", "--workers", "1"]
