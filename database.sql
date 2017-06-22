create database hibernate;
use hibernate;

CREATE TABLE `Agent` (
  `id` varchar(255) NOT NULL,
  `CREATED_DATE` date DEFAULT NULL,
  `UPDATED_DATE` date DEFAULT NULL,
  `CONCURRENT_PROC` int(11) DEFAULT NULL,
  `SESSION_ID` varchar(100) DEFAULT NULL,
  `SESSION_DATE` datetime DEFAULT NULL,
  `STATUS` varchar(50) DEFAULT NULL,
  `AGENT_NAME` varchar(50) DEFAULT NULL,
  `AGENT_ZONE` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

alter table Agent add unique UK_NAME_ZONE(AGENT_NAME, AGENT_ZONE);
alter table Agent add index (STATUS);


CREATE TABLE `Cmd` (
  `id` varchar(255) NOT NULL,
  `CREATED_DATE` datetime DEFAULT NULL,
  `UPDATED_DATE` datetime DEFAULT NULL,
  `FINISHED_DATE` datetime DEFAULT NULL,
  `CMD` varchar(255) DEFAULT NULL,
  `CMD_RESULT_ID` varchar(255) DEFAULT NULL,
  `TIMEOUT` bigint(20) DEFAULT NULL,
  `PRIORITY` int(11) DEFAULT NULL,
  `WORKING_DIR` varchar(255) DEFAULT NULL,
  `OUTPUT_ENV_FILTER` varchar(255) DEFAULT NULL,
  `LOG_PATHS` longtext,
  `INPUTS` longtext,
  `AGENT_ZONE` varchar(50) DEFAULT NULL,
  `AGENT_NAME` varchar(50) DEFAULT NULL,
  `TYPE` varchar(50) DEFAULT NULL,
  `STATUS` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
alter table Cmd add index IX_NAME_ZONE(AGENT_NAME, AGENT_ZONE);
alter table Cmd add index (CMD_RESULT_ID);


CREATE TABLE `CmdResult` (
  `id` varchar(255) NOT NULL,
  `PROCESS_ID` int(11) DEFAULT NULL,
  `EXIT_VALUE` int(11) DEFAULT NULL,
  `DURATION` bigint(20) DEFAULT NULL,
  `TOTAL_DURATION` bigint(20) DEFAULT NULL,
  `START_TIME` datetime DEFAULT NULL,
  `EXECUTED_TIME` datetime DEFAULT NULL,
  `FINISH_TIME` datetime DEFAULT NULL,
  `CMD_ID` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
alter table CmdResult add index (CMD_ID);

