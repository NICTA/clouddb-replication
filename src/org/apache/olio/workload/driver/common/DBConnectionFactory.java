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

import java.sql.*;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class DBConnectionFactory {

    private static Logger logger =
            Logger.getLogger(DBConnectionFactory.class.getName());
    private static final String DB_NAME = "olio";
    private static final String DB_USER = "olio";
    private static final String DB_PASS = "olio";
    protected String connectionURL;
    protected Connection conn;
    protected boolean closed = false;

    protected DBConnectionFactory(String dbhost) {
        connectionURL = String.format("jdbc:mysql:replication://%s/%s?user=%s&password=%s"
                + "&autoReconnect=true&roundRobinLoadBalance=true",
                dbhost, DB_NAME, DB_USER, DB_PASS);
    }

    abstract boolean ensureConnection();

    abstract boolean resetConnection();

    public Connection createConnection() {
        if (conn == null) {
            ensureConnection();
        }
        return conn;
    }

    void close() throws SQLException {
        closed = true;
        if (conn != null) {
            conn.close();
        }
    }
}
