from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional

from notifier import send_notification

app = FastAPI()


class NotificationRequest(BaseModel):
    message: str
    chat_ids: Optional[List[int]] = None


@app.post("/notify")
async def notify(req: NotificationRequest):
    await send_notification(req.message, req.chat_ids)
    return {"status": "ok"}