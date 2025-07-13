FROM python:3.12-slim
RUN pip install --no-cache-dir pip setuptools wheel spacy
WORKDIR /workspace
COPY annotate.py ./
CMD ["python", "annotate.py", "new"]
