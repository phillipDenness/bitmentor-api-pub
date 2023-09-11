# From https://github.com/DSpace-Labs/dspace-dev-docker/tree/master/postgres
FROM postgres

ENV POSTGRES_DB postgres
ENV POSTGRES_USER postgres
ENV POSTGRES_PASSWORD testpassword

COPY install-pgcrypto.sh /docker-entrypoint-initdb.d/
