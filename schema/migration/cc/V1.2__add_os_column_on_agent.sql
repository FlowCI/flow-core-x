USE flow_cc_db;

ALTER TABLE agent ADD COLUMN os varchar(50) DEFAULT NULL AFTER token;