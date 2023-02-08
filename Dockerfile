FROM amazoncorretto:17-alpine AS base

ENV POSTGRES_PASSWORD=1234

RUN echo 'http://dl-cdn.alpinelinux.org/alpine/v3.6/main' >> /etc/apk/repositories
RUN echo 'http://dl-cdn.alpinelinux.org/alpine/v3.6/community' >> /etc/apk/repositories

RUN apk update
RUN apk --no-cache add \
    curl \
    java-postgresql-jdbc \
    postgresql \
    mongodb \
    mongodb-tools

# Set up gradle

RUN mkdir -p /gradle_zip
RUN curl -L https://services.gradle.org/distributions/gradle-7.2-bin.zip --output /gradle_zip/gradle-7.2-bin.zip

# Set up database

RUN mkdir -p /run/postgresql
RUN chown postgres:postgres /run/postgresql

RUN mkdir -p /var/lib/postgresql/data
RUN chown postgres:postgres /var/lib/postgresql/data
RUN chmod 0700 /var/lib/postgresql/data

# Set up MongoDB database
RUN mkdir -p /data/db/
RUN chown 'root' /data/db

# Copy app to image

RUN mkdir -p /app
WORKDIR /app

COPY . .
RUN mv -f gradle/wrapper/gradle-wrapper.docker.properties gradle/wrapper/gradle-wrapper.properties

RUN mv init_db.sh /init_db.sh
RUN chmod 777 /init_db.sh

RUN chmod +x *.sh gradlew

EXPOSE 80
EXPOSE 1099

CMD ["./run.sh"]

HEALTHCHECK --interval=1s --timeout=1s --start-period=5s --retries=50 CMD cat /tmp/registered

FROM base as prod

RUN ./gradlew assemble
