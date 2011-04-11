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
 * $Id: DBWriteConnectionFactory.java,v 0.0.0.1 2011/03/22 10:59AM $
 */
package org.apache.olio.workload.driver.common;

import java.sql.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author liang
 */
public class DBWriteConnectionFactory extends DBConnectionFactory {

    private static Logger logger =
            Logger.getLogger(DBWriteConnectionFactory.class.getName());

    public DBWriteConnectionFactory(String dbhost) {
        super(dbhost);
    }

    @Override
    protected boolean ensureConnection() {
        if (closed) {
            logger.severe("Connection used after closure!");
            return false;
        }

        try {
            Class.forName("com.mysql.jdbc.ReplicationDriver").newInstance();
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(connectionURL);
                conn.setReadOnly(false);
                conn.setAutoCommit(false);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error connecting to DB", e);
            return false;
        }
        return true;
    }

    @Override
    protected boolean resetConnection() {
        if (closed) {
            logger.severe("Connection used after closure!");
            return false;
        }
        try {
            conn = DriverManager.getConnection(connectionURL);
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error connecting to DB", e);
            return false;
        }
        return true;
    }

    public void executeUpdate() throws SQLException {
        if (ensureConnection()) {
            conn.commit();
        }
    }
}
