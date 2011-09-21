/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.olio.workload.driver.operations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.olio.workload.driver.common.DBConnectionFactory;
import org.apache.olio.workload.driver.common.Operatable;

/**
 *
 * @author liang
 */
public class Login implements Operatable {

    // Strings
    private static final String CLASS_NAME = Login.class.getSimpleName() + "Operation";
    private static final String SUFFIX_NAME = " /* " + CLASS_NAME + " */";
    private static final String SELECT_USERS_1 = "SELECT * FROM `users` "
            + "WHERE (`users`.`username` = ? AND `users`.`password` = ?)  LIMIT 1";
    private static final String SELECT_INVITES = "SELECT * FROM `invites` "
            + "WHERE (`invites`.user_id_target = ?)";
    private static final String SELECT_EVENTS = "SELECT * FROM `events` INNER JOIN "
            + "`events_users` ON `events`.id = `events_users`.event_id "
            + "WHERE (`events_users`.user_id = ? AND (event_timestamp > ?)) "
            + "ORDER BY event_timestamp LIMIT 3";
    private static final String SELECT_USERS_2 = "SELECT * FROM `users`"
            + " WHERE (`users`.`id` = ?)";
    // Statements
    private PreparedStatement selectUsers1Stmt = null;
    private PreparedStatement selectInvitesStmt = null;
    private PreparedStatement selectEventsStmt = null;
    private PreparedStatement selectUsers2Stmt = null;
    // Input
    private Connection conn = null;
    private String username = null;
    private Integer randomId = null;
    // Output
    private int loginIdx = 0;

    public Login(String username, Integer randomId) {
        try {
            this.conn = DBConnectionFactory.getReadConnection();
        } catch (SQLException ex) {
            Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        this.username = username;
        this.randomId = randomId;
    }

    public void prepare() {

        try {
            selectUsers1Stmt = conn.prepareStatement(SELECT_USERS_1 + SUFFIX_NAME);
            selectInvitesStmt = conn.prepareStatement(SELECT_INVITES + SUFFIX_NAME);
            selectEventsStmt = conn.prepareStatement(SELECT_EVENTS + SUFFIX_NAME);
            selectUsers2Stmt = conn.prepareStatement(SELECT_USERS_2 + SUFFIX_NAME);
        } catch (SQLException ex) {
            Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }

    public void execute() {
        prepare();
        try {
            selectUsers1Stmt.setString(1, username);
            selectUsers1Stmt.setString(2, String.valueOf(randomId));
            ResultSet selectUsers1ResultSet = selectUsers1Stmt.executeQuery();
            if (selectUsers1ResultSet.next()) {
                loginIdx = -1;
            }
            selectUsers1ResultSet.close();

            if (loginIdx == -1) {
                selectInvitesStmt.setInt(1, randomId);
                selectInvitesStmt.executeQuery();

                selectEventsStmt.setInt(1, randomId);
                selectEventsStmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
                selectEventsStmt.executeQuery();

                selectUsers2Stmt.setInt(1, randomId);
                selectUsers2Stmt.executeQuery();
            } else {
                cleanup();
                return;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        cleanup();
    }

    public int getLoginIdx() {
        return loginIdx;
    }

    public void cleanup() {
        try {
            if (selectUsers1Stmt != null) {
                selectUsers1Stmt.close();
            }
            if (selectInvitesStmt != null) {
                selectInvitesStmt.close();
            }
            if (selectEventsStmt != null) {
                selectEventsStmt.close();
            }
            if (selectUsers2Stmt != null) {
                selectUsers2Stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(TagSearch.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }
}
