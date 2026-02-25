from transformers import AutoModelForCausalLM, AutoTokenizer

import os
from dotenv import load_dotenv


load_dotenv()  

MAX_NEW_TOKENS = os.getenv("MAX_NEW_TOKENS")
model_name = "Qwen/Qwen3-4B-Instruct-2507-FP8"

# load the tokenizer and the model
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForCausalLM.from_pretrained(
    model_name,
    torch_dtype="auto",
    device_map="auto"
)

# prepare the model input
prompt = "Ответь на вопрос, используя только контекст."

# TODO: Нужна функция для получения контекста из топ k релевантных документов базы знаний
context = ""

messages = [
    {
        "role": "system",
        "content": "Ты помощник службы поддержки. Отвечай строго на основе предоставленного контекста. Если информации недостаточно — скажи, что её нет."
    },

    {
        "role": "user",
        "content": 
        f"""
        Контекст:
        {context}

        Вопрос:
        {prompt}
        """
    }
]

text = tokenizer.apply_chat_template(
    messages,
    tokenize=False,
    add_generation_prompt=True,
)

model_inputs = tokenizer([text], return_tensors="pt").to(model.device)

# conduct text completion
generated_ids = model.generate(
    **model_inputs,
    max_new_tokens=MAX_NEW_TOKENS
)
output_ids = generated_ids[0][len(model_inputs.input_ids[0]):].tolist() 

content = tokenizer.decode(output_ids, skip_special_tokens=True)

print("content:", content)
