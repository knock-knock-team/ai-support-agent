import asyncio

from aiogram import Dispatcher, types
from aiogram.filters import Command

from config import API_HOST, API_PORT
# from storage import subscribe, unsubscribe
from database.repository import subscribe, unsubscribe
from notifier import create_bot
from api import app as fastapi_app

from database.connection import init_db
    
import uvicorn

bot = create_bot()
dp = Dispatcher()


@dp.message(Command("start"))
async def cmd_start(message: types.Message):
    subscribe(message.chat.id)
    await message.answer("Вы подписаны на уведомления")


@dp.message(Command("unsubscribe"))
async def cmd_unsubscribe(message: types.Message):
    unsubscribe(message.chat.id)
    await message.answer("Вы отписаны от уведомлений")


async def start_bot():
    await dp.start_polling(bot)


async def start_api():
    config = uvicorn.Config(
        fastapi_app,
        host=API_HOST,
        port=int(API_PORT),
        loop="asyncio"
    )
    server = uvicorn.Server(config)
    await server.serve()


async def main():
    await init_db()
    await asyncio.gather(
        start_bot(),
        start_api()
    )


if __name__ == "__main__":
    asyncio.run(main())