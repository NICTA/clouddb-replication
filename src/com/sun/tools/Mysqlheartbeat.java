/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.sun.com/cddl/cddl.html or
 * install_dir/legal/LICENSE
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at install_dir/legal/LICENSE.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Mysqlstats.java,v 1.6 2009/11/13 23:33:39 akara Exp $
 *
 * Copyright 2008-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.tools;

import com.sun.faban.harness.ConfigurationException;
import com.sun.faban.harness.Context;
import com.sun.faban.harness.Configure;
import com.sun.faban.harness.tools.Postprocess;
import com.sun.faban.harness.Start;
import com.sun.faban.harness.Stop;

import com.sun.faban.harness.tools.ToolContext;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQLHeartbeat implements a tool used for gathering the update delay from a
 * MySQL instance.
 */
public class Mysqlheartbeat {

    private static Logger logger =
            Logger.getLogger(Mysqlheartbeat.class.getName());
    /** The injected tool context. */
    @Context
    public ToolContext ctx;
    // Strings
    private static final String SHOW_SLAVE = "SHOW SLAVE STATUS";
    private static final String INSERT_HEARTBEATS = "INSERT INTO "
            + "heartbeats(master_time) VALUES (?)";
    private static final String SELECT_HEARTBEATS = "SELECT * FROM heartbeats "
            + "ORDER BY master_time DESC LIMIT 1";
    // Statements
    private PreparedStatement showSlaveStmt = null;
    private PreparedStatement insertHeartbeatsStmt = null;
    private PreparedStatement selectHeartbeatsStmt = null;
    // Input
    private Connection conn = null;
    private String toolName, logfile;
    private Integer intervalRead, intervalWrite;
    private Boolean isMaster = false;
    private Boolean runningFlag = true;
    // Output
    private List<MysqlheartbeatBean> results = new LinkedList<MysqlheartbeatBean>();
    private Integer queryCount = 0;

    /**
     * Configures the MySQLHeartbeat.
     */
    @Configure
    public void config() throws ConfigurationException {
        toolName = ctx.getToolName();
        String serverUser = ctx.getServiceProperty("user");
        if (serverUser == null || serverUser.trim().length() <= 0) {
            throw new ConfigurationException("MySQL serverUser property is not provided");
        }
        String serverPassword = ctx.getServiceProperty("password");
        if (serverPassword == null || serverPassword.trim().length() <= 0) {
            throw new ConfigurationException("MySQL serverPassword property is not provided");
        }
        intervalRead = Integer.valueOf(ctx.getServiceProperty("interval-read"));
        if (intervalRead == null || intervalRead == 0) {
            throw new ConfigurationException("MySQL intervalRead property is not provided");
        }
        intervalWrite = Integer.valueOf(ctx.getServiceProperty("interval-write"));
        if (intervalWrite == null || intervalWrite == 0) {
            throw new ConfigurationException("MySQL intervalWrite property is not provided");
        }

        String mysqlDb = "heartbeats";
        String url = String.format("jdbc:mysql://%s/%s?user=%s&password=%s",
                "localhost", mysqlDb, serverUser, serverPassword);
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            logger.fine("Setting up mysql connection for " + toolName);
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(url);
                showSlaveStmt = conn.prepareStatement(SHOW_SLAVE);
                insertHeartbeatsStmt = conn.prepareStatement(INSERT_HEARTBEATS);
                selectHeartbeatsStmt = conn.prepareStatement(SELECT_HEARTBEATS);
                logger.fine(toolName + " Configured");
            }
        } catch (Exception ex) {
            Logger.getLogger(Mysqlheartbeat.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        logfile = ctx.getOutputFile();
    }

    /**
     * Starts the MySQLHeartbeat.
     * @throws IOException Cannot execute the needed command
     * @throws InterruptedException Interrupted waiting for the stats commmand
     */
    @Start
    public void start() throws IOException, InterruptedException {
        try {
            ResultSet showSlaveResultSet = showSlaveStmt.executeQuery();
            if (!showSlaveResultSet.next()) {
                isMaster = true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Mysqlheartbeat.class.getName()).log(
                    Level.SEVERE, null, ex.getMessage());
        }
        if (isMaster) {
            logger.fine(toolName + " Started with INSERT" + " in start method");
            Thread writeThread = new Thread(new Runnable() {

                public void run() {
                    while (runningFlag == true) {
                        try {
                            insertHeartbeatsStmt.setLong(1, System.currentTimeMillis());
                            insertHeartbeatsStmt.executeUpdate();
                            queryCount++;
                            Thread.sleep(intervalWrite);
                        } catch (Exception ex) {
                            Logger.getLogger(Mysqlheartbeat.class.getName()).log(
                                    Level.SEVERE, null, ex.getMessage());
                        }
                    }
                }
            });
            writeThread.start();
        } else {
            logger.fine(toolName + " Started with SELECT" + " in start method");
            Thread readThread = new Thread(new Runnable() {

                public void run() {
                    while (runningFlag == true) {
                        try {
                            ResultSet selectHeartbeatsResultSet =
                                    selectHeartbeatsStmt.executeQuery();
                            if (selectHeartbeatsResultSet.next()) {
                                results.add(
                                        new MysqlheartbeatBean(
                                        selectHeartbeatsResultSet.getLong("master_time"),
                                        System.currentTimeMillis()));
                            }
                            queryCount++;
                            Thread.sleep(intervalRead);
                        } catch (Exception ex) {
                            Logger.getLogger(Mysqlheartbeat.class.getName()).log(
                                    Level.SEVERE, null, ex.getMessage());
                        }
                    }
                }
            });
            readThread.start();
        }
    }

    /**
     * Stops the MySQLHeartbeat.
     */
    @Stop
    public void stop() throws IOException, InterruptedException {
        logger.fine("Stopping tool " + toolName);
        runningFlag = false;
        try {
            if (!showSlaveStmt.isClosed()) {
                showSlaveStmt.close();
            }
            if (!insertHeartbeatsStmt.isClosed()) {
                insertHeartbeatsStmt.close();
            }
            if (!selectHeartbeatsStmt.isClosed()) {
                selectHeartbeatsStmt.close();
            }
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(Mysqlheartbeat.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }

    /**
     * Get final report by diffing the two time.
     */
    @Postprocess
    public void getReport() {
        try {
            BufferedWriter bufferWriter = new BufferedWriter(new FileWriter(logfile, true));
            if (isMaster) {
                bufferWriter.write("Number of writes: \t" + queryCount + "\n");
                bufferWriter.write("Interval of writes: \t" + intervalWrite + "\n");
            } else {
                bufferWriter.write("Number of reads: \t" + queryCount + "\n");
                bufferWriter.write("Interval of reads: \t" + intervalRead + "\n");
            }
            if (!results.isEmpty()) {
                bufferWriter.write("===========================================================\n");
                bufferWriter.write("master time,\t\tslave time,\t\ttime diff\n");
                Collections.sort(results);
                for (MysqlheartbeatBean mhb : results) {
                    bufferWriter.write(String.valueOf(mhb.getMaster()) + ",\t\t"
                            + String.valueOf(mhb.getSlave()) + ",\t\t"
                            + String.valueOf(mhb.getSlave() - mhb.getMaster()) + "\n");
                }
            }
            bufferWriter.flush();
            bufferWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(Mysqlheartbeat.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }

    private class MysqlheartbeatBean implements Comparable<MysqlheartbeatBean> {

        private Long master;
        private Long slave;

        public MysqlheartbeatBean(long master, long slave) {
            this.master = master;
            this.slave = slave;
        }

        public long getMaster() {
            return master;
        }

        public long getSlave() {
            return slave;
        }

        public int compareTo(MysqlheartbeatBean t) {
            if (this.master != t.master) {
                return this.master.compareTo(t.master);
            }
            else {
                return this.slave.compareTo(t.slave);
            }
        }
    }
}
