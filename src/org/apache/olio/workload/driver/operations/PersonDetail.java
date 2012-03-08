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
 * Web operation for viewing a person detail. All database transactions 
 * are from Olio Rails.
 * 
 * @author liang<Liang.Zhao@nicta.com.au>
 */
public class PersonDetail implements Operatable {

    // Strings
    private static final String CLASS_NAME = PersonDetail.class.getSimpleName() + "Operation";
    private static final String SUFFIX_NAME = " /* " + CLASS_NAME + " */";
    private static final String SELECT_USERS1 = "SELECT * FROM `users` "
            + "WHERE (`users`.`id` = ?)";
    private static final String SELECT_ADDRESSES = "SELECT * FROM `addresses` "
            + "WHERE (`addresses`.`id` = ?)";
    private static final String SELECT_IMAGES = "SELECT * FROM `images` "
            + "WHERE (`images`.`id` = ?)";
    private static final String SELECT_EVENTS = "SELECT * FROM `events` "
            + "WHERE (user_id = ?)  ORDER BY event_timestamp DESC";
    private static final String SELECT_USERS2 = "SELECT `users`.* FROM `users`  "
            + "INNER JOIN `invites` ON `users`.id = `invites`.user_id    "
            + "WHERE ((`invites`.user_id_target = ?) AND ((is_accepted = 1)))";
    private static final String SELECT_USERS3 = "SELECT `users`.* FROM `users`  "
            + "INNER JOIN `invites` ON `users`.id = `invites`.user_id_target    "
            + "WHERE ((`invites`.user_id = ?) AND ((is_accepted = 1)))";
    private static final String SELECT_INVITES1 = "SELECT * FROM `invites` "
            + "WHERE (`invites`.user_id_target = ?)";
    private static final String SELECT_INVITES2 = "SELECT * FROM `invites` "
            + "WHERE (`invites`.user_id = ?)";
    // Statements
    private PreparedStatement selectUsers1Stmt = null;
    private PreparedStatement selectAddressesStmt = null;
    private PreparedStatement selectImagesStmt = null;
    private PreparedStatement selectEventsStmt = null;
    private PreparedStatement selectUsers2Stmt = null;
    private PreparedStatement selectUsers3Stmt = null;
    private PreparedStatement selectInvites1Stmt = null;
    private PreparedStatement selectInvites2Stmt = null;
    // Input
    private Connection conn = null;
    private Integer userId = 0;
    // Output
    private List<String> eventIds = new ArrayList<String>();
    private List<String> imageUrls = new ArrayList<String>();

    public PersonDetail(Integer userId) {
        try {
            this.conn = DBConnectionFactory.getReadConnection();
        } catch (SQLException ex) {
            Logger.getLogger(PersonDetail.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        this.userId = userId;
    }

    public void prepare() {
        try {
            selectUsers1Stmt = conn.prepareStatement(SELECT_USERS1 + SUFFIX_NAME);
            selectAddressesStmt = conn.prepareStatement(SELECT_ADDRESSES + SUFFIX_NAME);
            selectImagesStmt = conn.prepareStatement(SELECT_IMAGES + SUFFIX_NAME);
            selectEventsStmt = conn.prepareStatement(SELECT_EVENTS + SUFFIX_NAME);
            selectUsers2Stmt = conn.prepareStatement(SELECT_USERS2 + SUFFIX_NAME);
            selectUsers3Stmt = conn.prepareStatement(SELECT_USERS3 + SUFFIX_NAME);
            selectInvites1Stmt = conn.prepareStatement(SELECT_INVITES1 + SUFFIX_NAME);
            selectInvites2Stmt = conn.prepareStatement(SELECT_INVITES2 + SUFFIX_NAME);
        } catch (SQLException ex) {
            Logger.getLogger(PersonDetail.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }

    public void execute() {
        prepare();
        List<Integer> imgIds = new ArrayList<Integer>();
        try {
            int addrIdx = -1, imgIdx = -1;
            selectUsers1Stmt.setInt(1, userId);
            ResultSet selectUsers1ResultSet = selectUsers1Stmt.executeQuery();
            if (selectUsers1ResultSet.next()) {
                addrIdx = selectUsers1ResultSet.getInt("address_id");
                imgIdx = selectUsers1ResultSet.getInt("image_id");
            }
            selectUsers1ResultSet.close();

            selectAddressesStmt.setInt(1, addrIdx);
            selectAddressesStmt.executeQuery();

            selectImagesStmt.setInt(1, imgIdx);
            ResultSet selectImagesResultSet = selectImagesStmt.executeQuery();
            if (selectImagesResultSet.next()) {
                imageUrls.add(selectImagesResultSet.getString("filename"));
            }
            selectImagesResultSet.close();

            selectEventsStmt.setInt(1, userId);
            ResultSet selectEventsResultSet = selectEventsStmt.executeQuery();
            while (selectEventsResultSet.next()) {
                eventIds.add(String.valueOf(selectEventsResultSet.getInt("id")));
                imgIds.add(selectEventsResultSet.getInt("image_id"));
            }
            selectEventsResultSet.close();

            selectUsers2Stmt.setInt(1, userId);
            selectUsers2Stmt.executeQuery();

            selectUsers3Stmt.setInt(1, userId);
            selectUsers3Stmt.executeQuery();

            selectInvites1Stmt.setInt(1, userId);
            selectInvites1Stmt.executeQuery();

            selectInvites2Stmt.setInt(1, userId);
            selectInvites2Stmt.executeQuery();

            for (Integer imgId : imgIds) {
                selectImagesStmt.setInt(1, imgId);
                selectImagesResultSet = selectImagesStmt.executeQuery();
                if (selectImagesResultSet.next()) {
                    imageUrls.add(selectImagesResultSet.getString("filename"));
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(PersonDetail.class.getName()).log(Level.SEVERE, null, ex.getMessage());
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
            if (selectUsers1Stmt != null) {
                selectUsers1Stmt.close();
            }
            if (selectAddressesStmt != null) {
                selectAddressesStmt.close();
            }
            if (selectImagesStmt != null) {
                selectImagesStmt.close();
            }
            if (selectEventsStmt != null) {
                selectEventsStmt.close();
            }
            if (selectUsers2Stmt != null) {
                selectUsers2Stmt.close();
            }
            if (selectUsers3Stmt != null) {
                selectUsers3Stmt.close();
            }
            if (selectInvites1Stmt != null) {
                selectInvites1Stmt.close();
            }
            if (selectInvites2Stmt != null) {
                selectInvites2Stmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(TagSearch.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }
}
