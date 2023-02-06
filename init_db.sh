#!/bin/bash

if [ ! -f /var/lib/postgresql/data/pg_hba.conf ]
then
    ## Initialize DB
    initdb -D /var/lib/postgresql/data
    ## Allow external connections
    echo "host all  all    0.0.0.0/0  md5" >> /var/lib/postgresql/data/pg_hba.conf
    echo "listen_addresses='*'" >> /var/lib/postgresql/data/postgresql.conf
fi

## Set user and password
pg_ctl start -D /var/lib/postgresql/data
psql -U postgres -tc "SELECT 1 FROM pg_database WHERE datname = 'main'" | grep -q 1 || psql -U postgres -c "CREATE DATABASE main"
psql -c "ALTER USER postgres WITH ENCRYPTED PASSWORD '$POSTGRES_PASSWORD';"
