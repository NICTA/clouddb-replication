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
    private List<String> imageUrls = new ArrayList<String>();

    public EventDetail(DBConnectionFactory dbConn, String eventId) {
        this.conn = dbConn.createConnection();
        this.eventId = Integer.parseInt(eventId);
    }

    public void prepare() {
        try {
            selectEventsStmt = conn.prepareStatement(SELECT_EVENTS);
            selectImagesStmt = conn.prepareStatement(SELECT_IMAGES);
            selectDocumentsStmt = conn.prepareStatement(SELECT_DOCUMENTS);
            selectCommentsStmt = conn.prepareStatement(SELECT_COMMENTS);
            selectUsers1Stmt = conn.prepareStatement(SELECT_USERS1);
            selectAddressesStmt = conn.prepareStatement(SELECT_ADDRESSES);
            selectUsers2Stmt = conn.prepareStatement(SELECT_USERS2);
            countTagsStmt = conn.prepareStatement(COUNT_TAGS);
            selectTagsStmt = conn.prepareStatement(SELECT_TAGS);
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

            selectImagesStmt.setInt(1, imgIdx);
            ResultSet selectImagesResultSet = selectImagesStmt.executeQuery();
            while (selectImagesResultSet.next()) {
                imageUrls.add(selectImagesResultSet.getString("filename"));
            }

            selectDocumentsStmt.setInt(1, docIdx);
            selectDocumentsStmt.executeQuery();

            selectCommentsStmt.setInt(1, eventId);
            ResultSet selectCommentsResultSet = selectCommentsStmt.executeQuery();
            while (selectCommentsResultSet.next()) {
                userIds.add(selectCommentsResultSet.getInt("user_id"));
            }

            String userIdList = new String();
            if (userIds.size() > 0) {
                userIdList += "?";
                for (int i = 1; i < userIds.size(); i++) {
                    userIdList += ",?";
                }
                String selectUsers1Replaced = SELECT_USERS1.replace("#USER_ID_LIST#", userIdList);
                selectUsers1Stmt = conn.prepareStatement(selectUsers1Replaced);
                for (int i = 0; i < userIds.size(); i++) {
                    selectUsers1Stmt.setInt(i + 1, userIds.get(i));
                }
                selectUsers1Stmt.executeQuery();
            }

            selectAddressesStmt.setInt(1, addrIdx);
            selectAddressesStmt.executeQuery();

            selectUsers2Stmt.setInt(1, eventId);
            selectUsers2Stmt.executeQuery();

            countTagsStmt.setInt(1, eventId);
            countTagsStmt.executeQuery();

            selectTagsStmt.setInt(1, eventId);
            selectTagsStmt.executeQuery();
        } catch (SQLException ex) {
            Logger.getLogger(EventDetail.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        cleanup();
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void cleanup() {
        try {
            if (!selectEventsStmt.isClosed()) {
                selectEventsStmt.close();
            }
            if (!selectImagesStmt.isClosed()) {
                selectImagesStmt.close();
            }
            if (!selectDocumentsStmt.isClosed()) {
                selectDocumentsStmt.close();
            }
            if (!selectCommentsStmt.isClosed()) {
                selectCommentsStmt.close();
            }
            if (!selectUsers1Stmt.isClosed()) {
                selectUsers1Stmt.close();
            }
            if (!selectAddressesStmt.isClosed()) {
                selectAddressesStmt.close();
            }
            if (!selectUsers2Stmt.isClosed()) {
                selectUsers2Stmt.close();
            }
            if (!countTagsStmt.isClosed()) {
                countTagsStmt.close();
            }
            if (!selectTagsStmt.isClosed()) {
                selectTagsStmt.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(TagSearch.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }
}
