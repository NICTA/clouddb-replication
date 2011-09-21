/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.olio.workload.driver.operations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.olio.workload.driver.common.DBConnectionFactory;
import org.apache.olio.workload.driver.common.Operatable;

/**
 *
 * @author liang
 */
public class EventDetail implements Operatable {

    // Strings
    private static final String CLASS_NAME = EventDetail.class.getSimpleName() + "Operation";
    private static final String SUFFIX_NAME = " /* " + CLASS_NAME + " */";
    private static final String SELECT_EVENTS = "SELECT * FROM `events` "
            + "WHERE (`events`.`id` = ?)";
    private static final String SELECT_IMAGES = "SELECT * FROM `images` "
            + "WHERE (`images`.`id` = ?)";
    private static final String SELECT_DOCUMENTS = "SELECT * FROM `documents` "
            + "WHERE (`documents`.`id` = ?)";
    private static final String SELECT_COMMENTS = "SELECT `comments`.* FROM `comments` "
            + "WHERE (`comments`.event_id = ?)";
    private static final String SELECT_USERS1 = "SELECT * FROM `users` "
            + "WHERE (`users`.`id` IN (#USER_ID_LIST#))";
    private static final String SELECT_ADDRESSES = "SELECT * FROM `addresses` "
            + "WHERE (`addresses`.`id` = ?)";
    private static final String SELECT_USERS2 = "SELECT * FROM `users`  "
            + "INNER JOIN `events_users` "
            + "ON `users`.id = `events_users`.user_id "
            + "WHERE (`events_users`.event_id = ? )  LIMIT 20";
    private static final String COUNT_TAGS = "SELECT count(*) AS count_all "
            + "FROM `tags`  INNER JOIN `taggings` "
            + "ON `tags`.id = `taggings`.tag_id    "
            + "WHERE ((`taggings`.taggable_id = ?) "
            + "AND (`taggings`.taggable_type = 'Event'))";
    private static final String SELECT_TAGS = "SELECT `tags`.* "
            + "FROM `tags`  INNER JOIN `taggings` "
            + "ON `tags`.id = `taggings`.tag_id    "
            + "WHERE ((`taggings`.taggable_id = ?) "
            + "AND (`taggings`.taggable_type = 'Event'))";
    // Statements
    private PreparedStatement selectEventsStmt = null;
    private PreparedStatement selectImagesStmt = null;
    private PreparedStatement selectDocumentsStmt = null;
    private PreparedStatement selectCommentsStmt = null;
    private PreparedStatement selectUsers1Stmt = null;
    private PreparedStatement selectAddressesStmt = null;
    private PreparedStatement selectUsers2Stmt = null;
    private PreparedStatement countTagsStmt = null;
    private PreparedStatement selectTagsStmt = null;
    // Input
    private Connection conn = null;
    private Integer eventId = 0;
    // Output
    private List<Integer> attendeesList = new ArrayList<Integer>();

    public EventDetail(String eventId) {
        try {
            this.conn = DBConnectionFactory.getReadConnection();
        } catch (SQLException ex) {
            Logger.getLogger(EventDetail.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        this.eventId = Integer.parseInt(eventId);
    }

    public void prepare() {
        try {
            selectEventsStmt = conn.prepareStatement(SELECT_EVENTS + SUFFIX_NAME);
            selectImagesStmt = conn.prepareStatement(SELECT_IMAGES + SUFFIX_NAME);
            selectDocumentsStmt = conn.prepareStatement(SELECT_DOCUMENTS + SUFFIX_NAME);
            selectCommentsStmt = conn.prepareStatement(SELECT_COMMENTS + SUFFIX_NAME);
            // selectUsers1Stmt is defined in execute().
            selectUsers1Stmt = null;
            selectAddressesStmt = conn.prepareStatement(SELECT_ADDRESSES + SUFFIX_NAME);
            selectUsers2Stmt = conn.prepareStatement(SELECT_USERS2 + SUFFIX_NAME);
            countTagsStmt = conn.prepareStatement(COUNT_TAGS + SUFFIX_NAME);
            selectTagsStmt = conn.prepareStatement(SELECT_TAGS + SUFFIX_NAME);
        } catch (SQLException ex) {
            Logger.getLogger(EventDetail.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }

    public void execute() {
        prepare();
        List<Integer> userIds = new ArrayList<Integer>();
        try {
            int imgIdx = -1, docIdx = -1, addrIdx = -1;
            selectEventsStmt.setInt(1, eventId);
            ResultSet selectEventsResultSet = selectEventsStmt.executeQuery();
            if (selectEventsResultSet.next()) {
                imgIdx = selectEventsResultSet.getInt("image_id");
                docIdx = selectEventsResultSet.getInt("document_id");
                addrIdx = selectEventsResultSet.getInt("address_id");
            }
            selectEventsResultSet.close();

            selectImagesStmt.setInt(1, imgIdx);
            selectImagesStmt.executeQuery();
            
            selectDocumentsStmt.setInt(1, docIdx);
            selectDocumentsStmt.executeQuery();

            selectCommentsStmt.setInt(1, eventId);
            ResultSet selectCommentsResultSet = selectCommentsStmt.executeQuery();
            while (selectCommentsResultSet.next()) {
                userIds.add(selectCommentsResultSet.getInt("user_id"));
            }
            selectCommentsResultSet.close();

            String userIdList = new String();
            if (userIds.size() > 0) {
                userIdList += "?";
                for (int i = 1; i < userIds.size(); i++) {
                    userIdList += ",?";
                }
                String selectUsers1Replaced = SELECT_USERS1.replace("#USER_ID_LIST#", userIdList);
                selectUsers1Stmt = conn.prepareStatement(selectUsers1Replaced + SUFFIX_NAME);
                for (int i = 0; i < userIds.size(); i++) {
                    selectUsers1Stmt.setInt(i + 1, userIds.get(i));
                }
                selectUsers1Stmt.executeQuery();
            }

            selectAddressesStmt.setInt(1, addrIdx);
            selectAddressesStmt.executeQuery();

            selectUsers2Stmt.setInt(1, eventId);
            ResultSet selectUsers2ResultSet = selectUsers2Stmt.executeQuery();
            if (selectUsers2ResultSet.next()) {
                attendeesList.add(selectUsers2ResultSet.getInt("user_id"));
            }
            selectUsers2ResultSet.close();

            countTagsStmt.setInt(1, eventId);
            countTagsStmt.executeQuery();

            selectTagsStmt.setInt(1, eventId);
            selectTagsStmt.executeQuery();
        } catch (SQLException ex) {
            Logger.getLogger(EventDetail.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        cleanup();
    }

    public List<Integer> getAttendeeList() {
        return attendeesList;
    }

    public void cleanup() {
        try {
            if (selectEventsStmt != null) {
                selectEventsStmt.close();
            }
            if (selectImagesStmt != null) {
                selectImagesStmt.close();
            }
            if (selectDocumentsStmt != null) {
                selectDocumentsStmt.close();
            }
            if (selectCommentsStmt != null) {
                selectCommentsStmt.close();
            }
            if (selectUsers1Stmt != null) {
                selectUsers1Stmt.close();
            }
            if (selectAddressesStmt != null) {
                selectAddressesStmt.close();
            }
            if (selectUsers2Stmt != null) {
                selectUsers2Stmt.close();
            }
            if (countTagsStmt != null) {
                countTagsStmt.close();
            }
            if (selectTagsStmt != null) {
                selectTagsStmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(TagSearch.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }
}
