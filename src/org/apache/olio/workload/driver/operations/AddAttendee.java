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
public class AddAttendee implements Operatable {

    // Strings
    private static final String SELECT_USERS = "SELECT `users`.id FROM `users`  "
            + "INNER JOIN `events_users` "
            + "ON `users`.id = `events_users`.user_id "
            + "WHERE (`users`.`id` = ?) "
            + "AND (`events_users`.event_id = ? )  LIMIT 1;";
    private static final String INSERT_EVENTS_USERS = "INSERT INTO `events_users` "
            + "(`event_id`, `user_id`) VALUES (?, ?)";
    private static final String SELECT_USERS2 = "SELECT * FROM `users`  "
            + "INNER JOIN `events_users` "
            + "ON `users`.id = `events_users`.user_id "
            + "WHERE (`events_users`.event_id = ? )  LIMIT 20";
    // Statements
    private PreparedStatement selectUsersStmt = null;
    private PreparedStatement insertEventsUsersStmt = null;
    private PreparedStatement selectUsers2Stmt = null;
    // Input
    private Connection conn = null;
    private Integer eventId = 0;
    private Integer userId = 0;
    // Output
    private Boolean success = false;

    public AddAttendee(String eventId, Integer userId) {
        try {
            this.conn = DBConnectionFactory.getWriteConnection();
        } catch (SQLException ex) {
            Logger.getLogger(AddAttendee.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        this.eventId = Integer.parseInt(eventId);
        this.userId = userId;
    }

    public void prepare() {
        try {
            selectUsersStmt = conn.prepareStatement(SELECT_USERS);
            insertEventsUsersStmt = conn.prepareStatement(INSERT_EVENTS_USERS);
            selectUsers2Stmt = conn.prepareStatement(SELECT_USERS2);
        } catch (SQLException ex) {
            Logger.getLogger(AddAttendee.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }

    public void execute() {
        prepare();
        try {
            boolean usersExisted = false;
            selectUsersStmt.setInt(1, userId);
            selectUsersStmt.setInt(2, eventId);
            ResultSet selectUsersResultSet = selectUsersStmt.executeQuery();
            if (selectUsersResultSet.next()) {
                usersExisted = true;
            }
            selectUsersResultSet.close();

            if (usersExisted) {
                conn.rollback();
                success = false;
            } else {
                insertEventsUsersStmt.setInt(1, eventId);
                insertEventsUsersStmt.setInt(2, userId);
                insertEventsUsersStmt.executeUpdate();

                selectUsers2Stmt.setInt(1, eventId);
                selectUsers2Stmt.executeQuery();

                conn.commit();
                success = true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(AddAttendee.class.getName()).log(Level.SEVERE, null, ex.getMessage());
            try {
                conn.rollback();
                success = false;
            } catch (SQLException ex1) {
                Logger.getLogger(AddAttendee.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        cleanup();
    }

    public void cleanup() {
        try {
            if (selectUsersStmt != null) {
                selectUsersStmt.close();
            }
            if (insertEventsUsersStmt != null) {
                insertEventsUsersStmt.close();
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

    public boolean getSuccess() {
        return success;
    }
}
