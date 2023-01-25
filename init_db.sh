#!/bin/bash

## Set user and password
pg_ctl start -D /var/lib/postgresql/data
psql -U postgres -tc "SELECT 1 FROM pg_database WHERE datname = 'main'" | grep -q 1 || psql -U postgres -c "CREATE DATABASE main"
psql -c "ALTER USER postgres WITH ENCRYPTED PASSWORD '$POSTGRES_PASSWORD';"
