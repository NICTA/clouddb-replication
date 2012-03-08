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
 * Web operation for opening the home page. All database transactions 
 * are from Olio Rails.
 * 
 * @author liang<Liang.Zhao@nicta.com.au>
 */
public class HomePage implements Operatable {

    // Strings
    private static final String CLASS_NAME = HomePage.class.getSimpleName() + "Operation";
    private static final String SUFFIX_NAME = " /* " + CLASS_NAME + " */";
    private static final String SELECT_EVENTS = "SELECT * FROM `events` "
            + "WHERE (event_date >= ?)  ORDER BY event_date LIMIT 0, 10";
    private static final String SELECT_ADDRESSES = "SELECT * FROM `addresses` "
            + "WHERE (`addresses`.`id` IN (?,?,?,?,?,?,?,?,?,?))";
    private static final String SELECT_IMAGES = "SELECT * FROM `images` "
            + "WHERE (`images`.`id` IN (?,?,?,?,?,?,?,?,?,?))";
    private static final String COUNT_EVENTS = "SELECT count(*) AS count_all "
            + "FROM `events` WHERE (event_date >= ?)";
    private static final String SELECT_TAGS = "SELECT tags.id AS id, tags.name AS name, "
            + "COUNT(*) AS count FROM tags, taggings, events "
            + "WHERE taggings.taggable_id = events.id "
            + "AND taggings.tag_id = tags.id "
            + "GROUP BY tags.name "
            + "ORDER BY count DESC LIMIT 50";
    // Statements
    private PreparedStatement selectEventsStmt = null;
    private PreparedStatement selectAddressesStmt = null;
    private PreparedStatement selectImagesStmt = null;
    private PreparedStatement countEventsStmt = null;
    private PreparedStatement selectTagsStmt = null;
    // Input
    private Connection conn = null;
    // Output
    private List<String> eventIds = new ArrayList<String>();
    private List<String> imageUrls = new ArrayList<String>();

    public HomePage() {
        try {
            this.conn = DBConnectionFactory.getReadConnection();
        } catch (SQLException ex) {
            Logger.getLogger(HomePage.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }

    public void prepare() {
        try {
            selectEventsStmt = conn.prepareStatement(SELECT_EVENTS + SUFFIX_NAME);
            selectAddressesStmt = conn.prepareStatement(SELECT_ADDRESSES + SUFFIX_NAME);
            selectImagesStmt = conn.prepareStatement(SELECT_IMAGES + SUFFIX_NAME);
            countEventsStmt = conn.prepareStatement(COUNT_EVENTS + SUFFIX_NAME);
            selectTagsStmt = conn.prepareStatement(SELECT_TAGS + SUFFIX_NAME);
        } catch (SQLException ex) {
            Logger.getLogger(HomePage.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }

    public void execute() {
        prepare();
        List<Integer> addressIds = new ArrayList<Integer>();
        List<Integer> imageIds = new ArrayList<Integer>();
        try {
            selectEventsStmt.setDate(1, new java.sql.Date(System.currentTimeMillis()));
            ResultSet selectEventsResultSet = selectEventsStmt.executeQuery();
            while (selectEventsResultSet.next()) {
                eventIds.add(String.valueOf(selectEventsResultSet.getInt("id")));
                addressIds.add(selectEventsResultSet.getInt("address_id"));
                imageIds.add(selectEventsResultSet.getInt("image_id"));
            }
            selectEventsResultSet.close();

            for (int i = 0; i < addressIds.size(); i++) {
                selectAddressesStmt.setInt(i + 1, addressIds.get(i));
            }
            for (int i = addressIds.size(); i < 10; i++) {
                selectAddressesStmt.setInt(i + 1, -1);
            }
            selectAddressesStmt.executeQuery();

            for (int i = 0; i < addressIds.size(); i++) {
                selectImagesStmt.setInt(i + 1, imageIds.get(i));
            }
            for (int i = addressIds.size(); i < 10; i++) {
                selectImagesStmt.setInt(i + 1, -1);
            }
            ResultSet selectImagesResultSet = selectImagesStmt.executeQuery();
            while (selectImagesResultSet.next()) {
                imageUrls.add(selectImagesResultSet.getString("filename"));
            }
            selectImagesResultSet.close();

            countEventsStmt.setDate(1, new java.sql.Date(System.currentTimeMillis()));
            countEventsStmt.executeQuery();

            selectTagsStmt.executeQuery();
        } catch (SQLException ex) {
            Logger.getLogger(HomePage.class.getName()).log(Level.SEVERE, null, ex.getMessage());
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
            if (selectAddressesStmt != null) {
                selectAddressesStmt.close();
            }
            if (selectImagesStmt != null) {
                selectImagesStmt.close();
            }
            if (countEventsStmt != null) {
                countEventsStmt.close();
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
