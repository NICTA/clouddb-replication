/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * $Id: DBConnectionFactory.java,v 0.0.0.1 2011/03/22 10:59AM $
 */
package org.apache.olio.workload.driver.common;

import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.dbcp.*;
import java.util.logging.Logger;

public class DBConnectionFactory {

    public static BasicDataSource bds;
    private static Logger logger =
            Logger.getLogger(DBConnectionFactory.class.getName());
    private static final String DB_NAME = "olio";
    private static final String DB_USER = "olio";
    private static final String DB_PASS = "olio";
    private static final String URL_STRING = "jdbc:mysql:replication://%s/%s?"
            + "user=%s&password=%s&autoReconnect=true&roundRobinLoadBalance=true";
    private static final Integer MAX_IDLE = -1;
    private static String connectionURL, dbhost;
    private static Integer maxActive = -1;

    public static void ensureConnection() throws SQLException {
        if (dbhost != null) {
            connectionURL = String.format(URL_STRING, dbhost,
                    DB_NAME, DB_USER, DB_PASS);
            bds = new BasicDataSource();
            bds.setDriverClassName("com.mysql.jdbc.ReplicationDriver");
            bds.setUrl(connectionURL);
            bds.setUsername(DB_USER);
            bds.setPassword(DB_PASS);
            bds.setMaxActive(maxActive);
            bds.setMaxIdle(MAX_IDLE);
        }
    }

    public static Connection getWriteConnection() throws SQLException {
        if (bds == null) {
            ensureConnection();
        }
        Connection conn = bds.getConnection();
        conn.setAutoCommit(false);
        conn.setReadOnly(false);
        return conn;
    }

    public static Connection getReadConnection() throws SQLException {
        if (bds == null) {
            ensureConnection();
        }
        Connection conn = bds.getConnection();
        conn.setAutoCommit(true);
        conn.setReadOnly(true);
        return conn;
    }

    public static void setDBHost(String hostList) {
        dbhost = hostList;
    }

    public static void setMaxActive(Integer num) {
        maxActive = num;
    }

    public static Integer getNumOfConnections() {
        if (bds == null) {
            return 0;
        }
        return bds.getNumActive() + bds.getNumIdle();
    }
}