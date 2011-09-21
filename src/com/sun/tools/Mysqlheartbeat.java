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
import java.text.SimpleDateFormat;
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
    private static final String SHOW_SLAVE = "SHOW SLAVE STATUS;";
    private static final String INSERT_HEARTBEATS = "SET SESSION binlog_format = 'STATEMENT'; "
            + "INSERT INTO heartbeats(sys_mill, db_micro) VALUES (?, now_microsec()) /* HeartbeatOperation */;";
    private static final String SELECT_HEARTBEATS = "SELECT * FROM heartbeats "
            + "ORDER BY sys_mill DESC;";
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
    private SimpleDateFormat millisFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS000");
    private SimpleDateFormat microsFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
    // Output
    private Thread writeThread;
    private List<MysqlheartbeatBean> results = null;
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
//        intervalRead = Integer.valueOf(ctx.getServiceProperty("interval-read"));
//        if (intervalRead == null || intervalRead == 0) {
//            throw new ConfigurationException("MySQL intervalRead property is not provided");
//        }
        intervalWrite = Integer.valueOf(ctx.getServiceProperty("interval-write"));
        if (intervalWrite == null || intervalWrite == 0) {
            throw new ConfigurationException("MySQL intervalWrite property is not provided");
        }

        String mysqlDb = "heartbeats";
        String url = String.format("jdbc:mysql://%s/%s?user=%s&password=%s&allowMultiQueries=true",
                "localhost", mysqlDb, serverUser, serverPassword);
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            logger.log(Level.FINE, "Setting up mysql connection for {0}", toolName);
            if (conn == null || conn.isClosed()) {
                conn = DriverManager.getConnection(url);
                showSlaveStmt = conn.prepareStatement(SHOW_SLAVE);
                insertHeartbeatsStmt = conn.prepareStatement(INSERT_HEARTBEATS);
                selectHeartbeatsStmt = conn.prepareStatement(SELECT_HEARTBEATS);
                logger.log(Level.FINE, "{0} Configured", toolName);
            }
        } catch (Exception ex) {
            Logger.getLogger(Mysqlheartbeat.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        logfile = ctx.getOutputFile();
    }

    /**
     * Starts the MySQLHeartbeat.
     * @throws IOException Cannot execute the needed command
     * @throws InterruptedException Interrupted waiting for the stats command
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
            logger.log(Level.FINE,"{0}" + " Started with INSERT in start method", toolName);
            writeThread = new Thread(new Runnable() {

                public void run() {
                    long startTime = System.currentTimeMillis();
                    while (runningFlag == true) {
                        try {
                            insertHeartbeatsStmt.setString(1,
                                    millisFormat.format(System.currentTimeMillis()));
                            insertHeartbeatsStmt.executeUpdate();
                            queryCount++;
                            while (startTime + intervalWrite > 
                                    System.currentTimeMillis()) {
                                Thread.sleep(Math.round(intervalWrite*0.1));
                            }
							startTime += intervalWrite;
                        } catch (Exception ex) {
                            Logger.getLogger(Mysqlheartbeat.class.getName()).log(
                                    Level.SEVERE, null, ex.getMessage());
                        }
                    }
                }
            });
            writeThread.start();
        }
    }

    /**
     * Stops the MySQLHeartbeat.
     */
    @Stop
    public void stop() throws IOException, InterruptedException {
        logger.fine("Stopping tool " + toolName);
        runningFlag = false;
        results = new ArrayList<MysqlheartbeatBean>();
        logger.fine(toolName + " Started with SELECT" + " in start method");
        try {
            ResultSet selectHeartbeatsResultSet =
                    selectHeartbeatsStmt.executeQuery();
            while (selectHeartbeatsResultSet.next()) {
                String sm = selectHeartbeatsResultSet.getString("sys_mill");
                String dm = selectHeartbeatsResultSet.getString("db_micro");
                results.add(
                        new MysqlheartbeatBean(
                        Long.valueOf(
                        microsFormat.parse(sm.substring(0, 23)).getTime() + sm.substring(23)),
                        Long.valueOf(
                        microsFormat.parse(dm.substring(0, 23)).getTime() + dm.substring(23))));
            }
        } catch (Exception ex) {
            Logger.getLogger(Mysqlheartbeat.class.getName()).log(
                    Level.SEVERE, null, ex.getMessage());
        }
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
     * Get final report by differing the two time.
     */
    @Postprocess
    public void getReport() {
        try {
            BufferedWriter bufferWriter = new BufferedWriter(new FileWriter(logfile, false));
            if (!results.isEmpty()) {
                bufferWriter.write("===========================================================\n");
                bufferWriter.write("sys_milli,\t\tdb_micro,\t\ttime_diff\n");
                Collections.sort(results);
                for (MysqlheartbeatBean mhb : results) {
                    bufferWriter.write(String.valueOf(mhb.getSysMilli()) + ",\t\t"
                            + String.valueOf(mhb.getDbMicro()) + ",\t\t"
                            + String.valueOf(mhb.getDbMicro() - mhb.getSysMilli()) + "\n");
                }
                bufferWriter.flush();
            }
            bufferWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(Mysqlheartbeat.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }

    private class MysqlheartbeatBean implements Comparable<MysqlheartbeatBean> {

        private Long sysMilli;
        private Long dbMicro;

        public MysqlheartbeatBean(long sysMilli, long dbMicro) {
            this.sysMilli = sysMilli;
            this.dbMicro = dbMicro;
        }

        public long getSysMilli() {
            return this.sysMilli;
        }

        public long getDbMicro() {
            return this.dbMicro;
        }

        public int compareTo(MysqlheartbeatBean t) {
            if (this.sysMilli != t.sysMilli) {
                return this.sysMilli.compareTo(t.sysMilli);
            } else {
                return this.dbMicro.compareTo(t.dbMicro);
            }
        }
    }
}
