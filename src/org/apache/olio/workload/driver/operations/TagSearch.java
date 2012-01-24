/**
 * Copyright 2011 National ICT Australia Limited
 *
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
 * Web operation for searching a tag. All database transactions are from 
 * Olio Rails.
 * 
 * @author liang<Liang.Zhao@nicta.com.au>
 */
public class TagSearch implements Operatable {

    // Strings
    private static final String CLASS_NAME = TagSearch.class.getSimpleName() + "Operation";
    private static final String SUFFIX_NAME = " /* " + CLASS_NAME + " */";
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

    public TagSearch(String tag) {
        try {
            this.conn = DBConnectionFactory.getReadConnection();
        } catch (SQLException ex) {
            Logger.getLogger(TagSearch.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        this.tag = tag;
    }

    public void prepare() {
        try {
            selectEventsStmt = conn.prepareStatement(SELECT_EVENTS + SUFFIX_NAME);
            selectImagesStmt = conn.prepareStatement(SELECT_IMAGES + SUFFIX_NAME);
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
            selectEventsResultSet.close();
            for (Integer imageId : imageIds) {
                selectImagesStmt.setInt(1, imageId);
                ResultSet selectImagesResultSet = selectImagesStmt.executeQuery();
                if (selectImagesResultSet.next()) {
                    imageUrls.add(selectImagesResultSet.getString("filename"));
                }
                selectImagesResultSet.close();
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
            if (selectEventsStmt != null) {
                selectEventsStmt.close();
            }
            if (selectImagesStmt != null) {
                selectImagesStmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(TagSearch.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }
}
