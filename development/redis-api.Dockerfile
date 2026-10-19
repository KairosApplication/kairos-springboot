# syntax=docker/dockerfile:1
# The named context is pinned to the upstream kairos-data commit in Compose.
FROM python:3.13-slim
WORKDIR /app
COPY --from=kairos_data /redis/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt \
    && groupadd --gid 10001 kairos \
    && useradd --uid 10001 --gid kairos --no-create-home kairos
COPY --from=kairos_data /redis/app ./app
USER 10001:10001
EXPOSE 8001
CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001"]
