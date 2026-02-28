from database.connection import get_pool


async def subscribe(chat_id: int):
    pool = get_pool()

    async with pool.acquire() as conn:
        await conn.execute("""
            INSERT INTO bot_subscribers(chat_id)
            VALUES($1)
            ON CONFLICT DO NOTHING
        """, chat_id)


async def unsubscribe(chat_id: int):
    pool = get_pool()

    async with pool.acquire() as conn:
        await conn.execute("""
            DELETE FROM bot_subscribers
            WHERE chat_id=$1
        """, chat_id)


async def get_subscribers():
    pool = get_pool()

    async with pool.acquire() as conn:
        rows = await conn.fetch("SELECT chat_id FROM bot_subscribers")
        return [r["chat_id"] for r in rows]