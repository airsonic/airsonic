-- *********************************************************************
-- SQL to roll back currently unexecuted changes
-- *********************************************************************
-- Change Log: classpath:liquibase/db-changelog.xml
-- Ran at: 21.10.19 19:38
-- Against: SA@jdbc:hsqldb:file:/var/airsonic/data/db/airsonic
-- Liquibase version: 3.6.3
-- *********************************************************************

-- Lock Database
UPDATE DATABASECHANGELOGLOCK SET LOCKED = TRUE, LOCKEDBY = 'limi.home (2a02:21b0:644d:2422:388b:e4bc:ec21:aef9%wlp2s0)', LOCKGRANTED = '2019-10-21 19:38:10.208' WHERE ID = 1 AND LOCKED = FALSE;

-- Release Database Lock
UPDATE DATABASECHANGELOGLOCK SET LOCKED = FALSE, LOCKEDBY = NULL, LOCKGRANTED = NULL WHERE ID = 1;

