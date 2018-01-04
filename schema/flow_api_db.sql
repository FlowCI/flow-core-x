# ------------------------------------------------------------

CREATE TABLE `action` (
  `name` varchar(100) NOT NULL,
  `alias` varchar(100) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `tag` varchar(10) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `credential` (
  `name` varchar(255) NOT NULL,
  `type` varchar(20) NOT NULL,
  `detail` longblob,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `flow` (
  `path` varchar(255) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `envs` longtext,
  `created_by` varchar(100) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `job` (
  `id` decimal(25,0) NOT NULL,
  `node_path` varchar(255) NOT NULL,
  `build_number` int(11) NOT NULL,
  `node_name` varchar(50) NOT NULL,
  `log_path` varchar(255) DEFAULT NULL,
  `job_status` varchar(20) NOT NULL,
  `job_category` varchar(20) NOT NULL,
  `session_id` varchar(255) DEFAULT NULL,
  `envs` longtext,
  `failure_msg` longtext,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_job_node_path_build_number` (`node_path`,`build_number`),
  KEY `idx_job_node_path` (`node_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `job_number` (
  `node_path` varchar(255) NOT NULL,
  `build_number` bigint NOT NULL,
  PRIMARY KEY (`node_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `job_yml_raw` (
  `job_id` decimal(25,0) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `file` longblob,
  PRIMARY KEY (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `message_setting` (
  `id` int(11) NOT NULL,
  `content` longtext,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `node_result` (
  `job_id` decimal(25,0) NOT NULL,
  `node_path` varchar(255) NOT NULL,
  `duration` bigint(20) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `exit_code` int(11) DEFAULT NULL,
  `cmd_id` varchar(255) DEFAULT NULL,
  `outputs` longtext,
  `log_path` varchar(255) DEFAULT NULL,
  `type` varchar(10) NOT NULL,
  `node_status` varchar(10) NOT NULL,
  `finished_at` timestamp NULL DEFAULT NULL,
  `started_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `failure_msg` longtext,
  `node_order` int(11) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`job_id`,`node_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `roles` (
  `id` int(11) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roles_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `roles_permissions` (
  `role_id` int(11) NOT NULL,
  `action` varchar(100) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`role_id`,`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `user` (
  `email` varchar(100) NOT NULL,
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(50) DEFAULT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`email`),
  UNIQUE KEY `uk_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `user_flow` (
  `flow_path` varchar(255) NOT NULL,
  `user_email` varchar(100) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`flow_path`,`user_email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `user_role` (
  `role_id` int(11) NOT NULL,
  `user_email` varchar(100) NOT NULL,
  `created_by` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`role_id`,`user_email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# ------------------------------------------------------------

CREATE TABLE `yml_raw` (
  `node_path` varchar(255) NOT NULL,
  `file` longblob,
  PRIMARY KEY (`node_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# ------------------------------------------------------------
CREATE TABLE `storage` (
  `id` varchar(100) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `extension` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# ------------------------------------------------------------

CREATE TABLE `artifact` (
  `id` int(11) NOT NULL,
  `job_id` decimal(25,0) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `url` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;