from qwen_agent import QwenAgent


agent = QwenAgent()

question = "Сколько будет 2+2?"
context = "Ты находишься в совершенно ином мире. здесь не работают классические принципы математики. В этом мире 1+1=5, а 2+2=6"

response = agent.generate(question=question, context=context, mode="qa")
print(response)


# для классификации контекст не нужен, в question передаем сообщение от пользователя
question = "Мне необходимо получить дополнительную информацию о том как правильно эксплуатировать мой объект"

response = agent.generate(question=question, mode="classification")
print(response)