FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    # Cache Hugging Face sur un volume : le modèle n'est téléchargé qu'une fois.
    HF_HOME=/models

WORKDIR /app

# Torch en version CPU (~200 Mo) au lieu de la version CUDA par défaut (~2,5 Go).
# Installé en premier pour que sentence-transformers ne tire pas la variante CUDA.
RUN pip install --no-cache-dir --index-url https://download.pytorch.org/whl/cpu torch==2.14.1

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

RUN useradd --create-home appuser && mkdir -p /models && chown appuser /models
USER appuser

EXPOSE 8000
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8000"]
