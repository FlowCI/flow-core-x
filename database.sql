create database flow_cc_db_local;
use flow_cc_db_local;

CREATE TABLE `agent` (
  `agent_name` varchar(50) NOT NULL,
  `agent_zone` varchar(50) NOT NULL,
  `created_date` date DEFAULT NULL,
  `updated_date` date DEFAULT NULL,
  `concurrent_proc` int(11) DEFAULT NULL,
  `session_id` varchar(255) DEFAULT NULL,
  `session_date` datetime DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`agent_name`,`agent_zone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table agent add unique UK_NAME_ZONE(agent_name, agent_zone);
alter table agent add index (status);
alter table agent add index (session_id);


CREATE TABLE `cmd` (
  `id` varchar(255) NOT NULL,
  `created_date` datetime DEFAULT NULL,
  `updated_date` datetime DEFAULT NULL,
  `finished_date` datetime DEFAULT NULL,
  `cmd` varchar(255) DEFAULT NULL,
  `timeout` bigint(20) DEFAULT NULL,
  `priority` int(11) DEFAULT NULL,
  `working_dir` varchar(255) DEFAULT NULL,
  `output_env_filter` varchar(255) DEFAULT NULL,
  `log_paths` longtext,
  `inputs` longtext,
  `agent_zone` varchar(50) DEFAULT NULL,
  `agent_name` varchar(50) DEFAULT NULL,
  `type` varchar(50) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table cmd add index IX_NAME_ZONE(agent_name, agent_zone);
alter table cmd add index (status);


CREATE TABLE `cmd_result` (
  `cmd_id` varchar(255) NOT NULL,
  `process_id` int(11) DEFAULT NULL,
  `exit_value` int(11) DEFAULT NULL,
  `duration` bigint(20) DEFAULT NULL,
  `total_duration` bigint(20) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `executed_time` datetime DEFAULT NULL,
  `finish_time` datetime DEFAULT NULL,
  PRIMARY KEY (`cmd_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table cmd_result add index (cmd_id);

