# ------------------------------------------------------------

CREATE TABLE `agent` (
  `agent_zone` varchar(50) NOT NULL,
  `agent_name` varchar(100) NOT NULL,
  `concurrent_proc` int(11) DEFAULT NULL,
  `session_id` varchar(50) DEFAULT NULL,
  `session_date` datetime DEFAULT NULL,
  `token` varchar(255) DEFAULT NULL,
  'os' varchar(50) DEFAULT NULL,
  `webhook` varchar(255) DEFAULT NULL,
  `status` varchar(10) NOT NULL,
  `created_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`agent_zone`,`agent_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `cmd` (
  `id` varchar(100) NOT NULL,
  `agent_zone` varchar(50) DEFAULT NULL,
  `agent_name` varchar(100) DEFAULT NULL,
  `type` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL,
  `cmd` longtext,
  `timeout` int(11) DEFAULT NULL,
  `session_id` varchar(36) DEFAULT NULL,
  `working_dir` varchar(255) DEFAULT NULL,
  `output_env_filter` longtext,
  `log_path` varchar(255) DEFAULT NULL,
  `inputs` longtext,
  `webhook` varchar(255) DEFAULT NULL,
  `extra` varchar(255) DEFAULT NULL,
  `retry` int(11) DEFAULT NULL,
  `created_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `cmd_result` (
  `cmd_id` varchar(100) NOT NULL,
  `process_id` int(11) DEFAULT NULL,
  `exit_value` int(11) DEFAULT NULL,
  `duration` bigint(20) DEFAULT NULL,
  `total_duration` bigint(20) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `executed_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  `output` longtext,
  `exceptions` longtext,
  PRIMARY KEY (`cmd_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;