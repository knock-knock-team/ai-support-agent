from transformers import pipeline

model = pipeline(model="r1char9/rubert-base-cased-russian-sentiment")
model("Привет, ты мне нравишься!")
# [{'label': 'positive', 'score': 0.8220236897468567}]

