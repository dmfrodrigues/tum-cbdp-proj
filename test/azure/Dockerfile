FROM python:3.8-slim

ARG TEST_SCRIPT

COPY $TEST_SCRIPT /test_script.py
RUN chmod 777 /test_script.py

CMD ["python", "test_script.py"]