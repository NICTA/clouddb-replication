/**
 * Copyright 2011 National ICT Australia Limited
 *
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
 */
package org.apache.olio.workload.driver.common;

import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.dbcp.*;
import java.util.logging.Logger;

/**
 * DBConnectionFactory implements a connection pool to deal with all database
 * connections.
 * 
 * @author liang<Liang.Zhao@nicta.com.au>
 */
public class DBConnectionFactory {

    public static BasicDataSource bds;
    private static Logger logger =
            Logger.getLogger(DBConnectionFactory.class.getName());
    private static String connectionURL = null;
    private static String dbDriver = null;
    private static final Integer MAX_IDLE = 0;
    private static Integer maxActive = -1;

    public static void ensureConnection() throws SQLException {
        if (connectionURL != null && dbDriver != null) {
            bds = new BasicDataSource();
            bds.setDriverClassName(dbDriver);
            bds.setUrl(connectionURL);
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

    public static void setDbDriver(String driver) {
        dbDriver = driver;
    }

    public static void setConnectionURL(String connURL) {
        connectionURL = connURL;
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