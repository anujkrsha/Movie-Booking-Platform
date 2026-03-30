-- init-db.sql
-- Runs automatically on first postgres container start.
-- The default database (userdb) is created by POSTGRES_DB env var;
-- we only need to create the remaining six here.

\c postgres

SELECT 'Creating moviedb' AS status;
CREATE DATABASE moviedb
    WITH OWNER = booking
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TEMPLATE = template0;

SELECT 'Creating theatredb' AS status;
CREATE DATABASE theatredb
    WITH OWNER = booking
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TEMPLATE = template0;

SELECT 'Creating showdb' AS status;
CREATE DATABASE showdb
    WITH OWNER = booking
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TEMPLATE = template0;

SELECT 'Creating bookingdb' AS status;
CREATE DATABASE bookingdb
    WITH OWNER = booking
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TEMPLATE = template0;

SELECT 'Creating paymentdb' AS status;
CREATE DATABASE paymentdb
    WITH OWNER = booking
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TEMPLATE = template0;

SELECT 'Creating offerdb' AS status;
CREATE DATABASE offerdb
    WITH OWNER = booking
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TEMPLATE = template0;

-- Grant full privileges on every database to the booking user
GRANT ALL PRIVILEGES ON DATABASE userdb    TO booking;
GRANT ALL PRIVILEGES ON DATABASE moviedb   TO booking;
GRANT ALL PRIVILEGES ON DATABASE theatredb TO booking;
GRANT ALL PRIVILEGES ON DATABASE showdb    TO booking;
GRANT ALL PRIVILEGES ON DATABASE bookingdb TO booking;
GRANT ALL PRIVILEGES ON DATABASE paymentdb TO booking;
GRANT ALL PRIVILEGES ON DATABASE offerdb   TO booking;

SELECT 'All databases created successfully.' AS status;
