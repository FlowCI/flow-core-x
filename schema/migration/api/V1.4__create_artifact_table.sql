USE flow_api_db;

CREATE TABLE `local_file_resource` (
  `id` varchar(100) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `extension` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `artifact` (
  `id` int(11) NOT NULL,
  `job_id` decimal(25,0) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `url` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;