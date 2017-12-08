USE flow_api_db;

CREATE TABLE `job_number` (
  `node_path` varchar(255) NOT NULL,
  `build_number` bigint NOT NULL,
  PRIMARY KEY (`node_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE job Modify column build_number bigint not null;