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
    // Statements
    private PreparedStatement selectUsersStmt = null;
    private PreparedStatement insertEventsUsersStmt = null;
    // Input
    private Connection conn = null;
    private Integer eventId = 0;
    private Integer userId = 0;
    // Output
    private Boolean success = false;

    public AddAttendee(DBConnectionFactory dbConn, String eventId, Integer userId) {
        this.conn = dbConn.createConnection();
        this.eventId = Integer.parseInt(eventId);
        this.userId = userId;
    }

    public void prepare() {
        try {
            selectUsersStmt = conn.prepareStatement(SELECT_USERS);
            insertEventsUsersStmt = conn.prepareStatement(INSERT_EVENTS_USERS);
        } catch (SQLException ex) {
            Logger.getLogger(AddAttendee.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }

    public void execute() {
        prepare();
        try {
            selectUsersStmt.setInt(1, userId);
            selectUsersStmt.setInt(2, eventId);
            ResultSet selectUsers2ResultSet = selectUsersStmt.executeQuery();
            if (!selectUsers2ResultSet.next()) {
                insertEventsUsersStmt.setInt(1, eventId);
                insertEventsUsersStmt.setInt(2, userId);
                insertEventsUsersStmt.executeUpdate();

                conn.commit();
                success = true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(AddAttendee.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        cleanup();
    }

    public void cleanup() {
        try {
            if (!selectUsersStmt.isClosed()) {
                selectUsersStmt.close();
            }
            if (!insertEventsUsersStmt.isClosed()) {
                insertEventsUsersStmt.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(TagSearch.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }

    public boolean getSuccess() {
        return success;
    }
}
