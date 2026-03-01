from sentiment_classification import SentimentService
from qwen_agent import QwenAgent


# анализ тональности 
service = SentimentService()

text = "Мне очень понравился сервис, всё работает отлично!"

result = service.predict(
    text=text,
    return_proba=True
)
print(result)

# генерация ответа пользователю на его сообщение
agent = QwenAgent()

question = "Сколько будет 2+2?"
context = "Ты находишься в совершенно ином мире. здесь не работают классические принципы математики. В этом мире 1+1=5, а 2+2=6"

response = agent.generate(question=question, context=context, mode="qa")
print(response)

# классификация входящего сообщения на категории
# для классификации контекст не нужен, в question передаем сообщение от пользователя
question = "Мне необходимо получить дополнительную информацию о том как правильно эксплуатировать мой объект"

response = agent.generate(question=question, mode="classification")
print(response)


# Извлечение информации из сообщения пользователя
question = """
Тип обращения: Обращение по продукции knock-knock
ФИО: Иванов Иван Иванович
Телефон: +79123684717
Email: example@mail.ru
Заводской номер прибора: 123123

Добрый день! Я сотрудник предприятия "ООО Живая Сталь". Мне необходимо получить дополнительную информацию о том как правильно эксплуатировать мой прибор - газотурбинное оборудование. Надеюсь на быстрое решение моего вопроса

Время: 2026-02-26T14:27:18Z"""

response = agent.generate(question=question, mode="extraction")
print(response)

# with open("result.json", "w", encoding="utf-8") as f:
#     json.dump(response, f, ensure_ascii=False, indent=2)
