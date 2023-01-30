FROM amazoncorretto:17-alpine AS peer

ENV POSTGRES_PASSWORD=1234

RUN apk update
RUN apk --no-cache add \
    curl \
    java-postgresql-jdbc \
    postgresql

# Set up gradle

RUN mkdir -p /gradle_zip
RUN curl -L https://services.gradle.org/distributions/gradle-7.2-bin.zip --output /gradle_zip/gradle-7.2-bin.zip

# Set up database

RUN mkdir -p /run/postgresql
RUN chown postgres:postgres /run/postgresql

RUN mkdir -p /var/lib/postgresql/data
RUN chown postgres:postgres /var/lib/postgresql/data
RUN chmod 0700 /var/lib/postgresql/data

USER postgres
## Initialize DB
RUN initdb -D /var/lib/postgresql/data
## Allow external connections
RUN echo "host all  all    0.0.0.0/0  md5" >> /var/lib/postgresql/data/pg_hba.conf
RUN echo "listen_addresses='*'" >> /var/lib/postgresql/data/postgresql.conf
## Done with initializing DB
USER root

# Copy app to image

RUN mkdir -p /app
WORKDIR /app

COPY . .

RUN mv init_db.sh /init_db.sh
RUN chmod 777 /init_db.sh

RUN chmod +x *.sh gradlew

RUN ./gradlew assemble

EXPOSE 80
EXPOSE 1099

ENV CBDP_LEADER leader
CMD exec ./run.join.sh "$CBDP_LEADER"

HEALTHCHECK --interval=1s --timeout=1s --start-period=5s --retries=50 CMD cat /tmp/registered

FROM peer as leader

CMD ["./run.sh"]
