FROM python:3.8-slim

ARG TEST_SCRIPT
ARG TEST_SCRIPT_ARG
ENV TEST_SCRIPT_ARG_ENV ${TEST_SCRIPT_ARG}

COPY $TEST_SCRIPT /test_script.py
RUN chmod 777 /test_script.py

CMD python test_script.py ${TEST_SCRIPT_ARG_ENV}