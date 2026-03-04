CREATE TYPE STATUS AS ENUM ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED');
CREATE CAST (varchar as STATUS) with inout as implicit;
CREATE CAST (STATUS as varchar) with inout as implicit;

CREATE TYPE DESTINATION AS ENUM ('KAFKA','RABBITMQ','SQS','HTTP');
CREATE CAST (varchar as DESTINATION) with inout as implicit;
CREATE CAST (DESTINATION as varchar) with inout as implicit;