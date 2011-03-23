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
public class TagSearch implements Operatable {

    // Strings
    private static final String SELECT_EVENTS = "SELECT events.* "
            + "FROM events, tags, taggings "
            + "WHERE events.id = taggings.taggable_id "
            + "AND taggings.taggable_type = 'Event' "
            + "AND taggings.tag_id = tags.id "
            + "AND tags.name LIKE ?";
    private static final String SELECT_IMAGES = "SELECT * FROM `images` "
            + "WHERE (`images`.`id` = ?)";
    // Statements
    private PreparedStatement selectEventsStmt = null;
    private PreparedStatement selectImagesStmt = null;
    // Input
    private Connection conn = null;
    private String tag = null;
    // Output
    private List<String> eventIds = new ArrayList<String>();
    private List<String> imageUrls = new ArrayList<String>();

    public TagSearch(DBConnectionFactory dbConn, String tag) {
        this.conn = dbConn.createConnection();
        this.tag = tag;
    }

    public void prepare() {
        try {
            selectEventsStmt = conn.prepareStatement(SELECT_EVENTS);
            selectImagesStmt = conn.prepareStatement(SELECT_IMAGES);
        } catch (SQLException ex) {
            Logger.getLogger(TagSearch.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }

    public void execute() {
        prepare();
        List<Integer> imageIds = new ArrayList<Integer>();
        try {
            selectEventsStmt.setString(1, tag);
            ResultSet selectEventsResultSet = selectEventsStmt.executeQuery();
            while (selectEventsResultSet.next()) {
                eventIds.add(String.valueOf(selectEventsResultSet.getInt("id")));
                imageIds.add(selectEventsResultSet.getInt("image_id"));
            }
            for (Integer imageId : imageIds) {
                selectImagesStmt.setInt(1, imageId);
                ResultSet selectImagesResultSet = selectImagesStmt.executeQuery();
                if (selectImagesResultSet.next()) {
                    imageUrls.add(selectImagesResultSet.getString("filename"));
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(TagSearch.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        cleanup();
    }

    public List<String> getEventIds() {
        return eventIds;
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
        } catch (SQLException ex) {
            Logger.getLogger(TagSearch.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }
}
