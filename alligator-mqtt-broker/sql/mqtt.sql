-- --------------------------------------------------------
-- 主机:                           192.168.100.115
-- 服务器版本:                        5.7.36 - MySQL Community Server (GPL)
-- 服务器操作系统:                      Linux
-- HeidiSQL 版本:                  12.2.0.6576
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- 导出 alligator_mqtt 的数据库结构
DROP DATABASE IF EXISTS `alligator_mqtt`;
CREATE DATABASE IF NOT EXISTS `alligator_mqtt` /*!40100 DEFAULT CHARACTER SET utf8 */;
USE `alligator_mqtt`;

-- 导出  表 alligator_mqtt.t_stored_message 结构
DROP TABLE IF EXISTS `t_stored_message`;
CREATE TABLE IF NOT EXISTS `t_stored_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `package_id` int(11) NOT NULL COMMENT '消息id',
  `topic` varchar(100) NOT NULL COMMENT 'topic名称',
  `qos` tinyint(4) NOT NULL COMMENT 'QoS',
  `payload` blob NOT NULL COMMENT '消息内容',
  `offset` bigint(20) unsigned NOT NULL COMMENT '消息偏移量offset',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8;

-- 数据导出被取消选择。

-- 导出  表 alligator_mqtt.t_user 结构
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE IF NOT EXISTS `t_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '逻辑主键',
  `username` varchar(32) NOT NULL COMMENT '用户名',
  `password` varchar(64) NOT NULL COMMENT '密码',
  `status` tinyint(4) NOT NULL COMMENT '状态',
  PRIMARY KEY (`id`),
  UNIQUE KEY `t_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户表';

-- 数据导出被取消选择。

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
