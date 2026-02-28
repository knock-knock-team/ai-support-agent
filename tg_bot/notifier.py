from aiogram import Bot
from config import BOT_TOKEN
# from storage import get_subscribers
from database.repository import get_subscribers


def create_bot():
    return Bot(token=BOT_TOKEN)

bot = create_bot()

async def send_notification(message: str, chat_ids=None):
    if chat_ids is None:
        chat_ids = get_subscribers()

    for chat_id in chat_ids:
        try:
            await bot.send_message(chat_id, message)
        except Exception:
            pass