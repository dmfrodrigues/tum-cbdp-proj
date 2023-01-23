FROM amazoncorretto:17-alpine

ENV GRADLE_USER_HOME=/gradle_home
RUN mkdir -p ${GRADLE_USER_HOME}

RUN mkdir -p /app
WORKDIR /app

COPY . .

RUN chmod +x *.sh

CMD ["./run.sh"]
