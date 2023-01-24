FROM amazoncorretto:17-alpine

RUN apk --no-cache add \
    curl

RUN mkdir -p /gradle_zip
RUN curl -L https://services.gradle.org/distributions/gradle-7.2-bin.zip --output /gradle_zip/gradle-7.2-bin.zip

RUN mkdir -p /app
WORKDIR /app

COPY . .

RUN chmod +x *.sh

CMD ["./run.sh"]
