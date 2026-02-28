import json
import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
FILE = os.path.join(BASE_DIR, "subscriptions.json")


def load_data():
    if not os.path.exists(FILE):
        return {"users": {}, "groups": {}}

    with open(FILE, "r", encoding="utf-8") as f:
        return json.load(f)


def save_data(data):
    with open(FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def subscribe(chat_id: int):
    data = load_data()

    if str(chat_id) not in data["users"]:
        data["users"][str(chat_id)] = True

    save_data(data)


def unsubscribe(chat_id: int):
    data = load_data()

    data["users"].pop(str(chat_id), None)

    save_data(data)


def get_subscribers():
    data = load_data()
    return list(map(int, data["users"].keys()))