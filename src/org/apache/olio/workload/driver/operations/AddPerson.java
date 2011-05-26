/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.olio.workload.driver.operations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.olio.workload.driver.common.DBConnectionFactory;
import org.apache.olio.workload.driver.common.Operatable;

/**
 *
 * @author liang
 */
public class AddPerson implements Operatable {

    // Strings
    private static final String SELECT_IMAGES1 = "SELECT `images`.id FROM `images` "
            + "WHERE (`images`.`filename` = BINARY ?)  LIMIT 1";
    private static final String INSERT_IMAGES1 = "INSERT INTO `images` "
            + "(`size`, `content_type`, `thumbnail`, `filename`, `height`, "
            + "`parent_id`, `width`) "
            + "VALUES(671520, 'image/jpeg; charset=ISO-8859-1', NULL, ?, 1124, "
            + "NULL, 1050)";
    private static final String SELECT_IMAGES2 = "SELECT * FROM `images` "
            + "WHERE (`images`.`parent_id` = ? AND `images`.`thumbnail` = 'thumb')  LIMIT 1";
    private static final String SELECT_IMAGES3 = "SELECT `images`.id FROM `images` "
            + "WHERE (`images`.`filename` = BINARY ?)  LIMIT 1";
    private static final String INSERT_IMAGES2 = "INSERT INTO `images` "
            + "(`size`, `content_type`, `thumbnail`, `filename`, `height`, "
            + "`parent_id`, `width`) "
            + "VALUES(17239, 'image/jpeg; charset=ISO-8859-1', 'thumb', ?, 120, ?, 112)";
    private static final String INSERT_ADDRESSES = "INSERT INTO `addresses` "
            + "(`city`, `zip`, `latitude`, `country`, `street1`, `street2`, `"
            + "longitude`, `state`) "
            + "VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_USERS = "SELECT `users`.id FROM `users` "
            + "WHERE (`users`.`username` = BINARY ?)  LIMIT 1";
    private static final String INSERT_USERS = "INSERT INTO `users` "
            + "(`image_id`, `created_at`, `updated_at`, `thumbnail`, `username`, "
            + "`lastname`, `timezone`, `address_id`, `firstname`, `telephone`, "
            + "`summary`, `password`, `email`) "
            + "VALUES(?, ?, ?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    // Statements
    private PreparedStatement selectImages1Stmt = null;
    private PreparedStatement insertImages1Stmt = null;
    private PreparedStatement selectImages2Stmt = null;
    private PreparedStatement selectImages3Stmt = null;
    private PreparedStatement insertImages2Stmt = null;
    private PreparedStatement insertAddressesStmt = null;
    private PreparedStatement selectUsersStmt = null;
    private PreparedStatement insertUsersStmt = null;
    // Input
    private Connection conn = null;
    private String[] parameters = null;
    private String[] addressArr = null;
    private Integer threadId = -1;
    // Output
    private String message = null;

    public AddPerson(String[] parameters, String[] addressArr, int threadId) {
        try {
            this.conn = DBConnectionFactory.getWriteConnection();
        } catch (SQLException ex) {
            Logger.getLogger(AddPerson.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
        this.parameters = parameters;
        this.addressArr = addressArr;
        this.threadId = threadId;
    }

    public void prepare() {
        try {
            selectImages1Stmt = conn.prepareStatement(SELECT_IMAGES1);
            insertImages1Stmt = conn.prepareStatement(INSERT_IMAGES1, Statement.RETURN_GENERATED_KEYS);
            selectImages2Stmt = conn.prepareStatement(SELECT_IMAGES2);
            selectImages3Stmt = conn.prepareStatement(SELECT_IMAGES3);
            insertImages2Stmt = conn.prepareStatement(INSERT_IMAGES2, Statement.RETURN_GENERATED_KEYS);
            insertAddressesStmt = conn.prepareStatement(INSERT_ADDRESSES, Statement.RETURN_GENERATED_KEYS);
            selectUsersStmt = conn.prepareStatement(SELECT_USERS);
            insertUsersStmt = conn.prepareStatement(INSERT_USERS, Statement.RETURN_GENERATED_KEYS);
        } catch (SQLException ex) {
            Logger.getLogger(AddPerson.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }

    public void execute() {
        prepare();
        Random generator = new Random(System.currentTimeMillis());
        String imagePrefix = String.valueOf(generator.nextInt(1000000000));
        try {
            int img1Idx = -1;
            selectImages1Stmt.setString(1, imagePrefix + threadId + "person.jpg");
            selectImages1Stmt.executeQuery();
            ResultSet selectImages1ResultSet = selectImages1Stmt.executeQuery();
            if (selectImages1ResultSet.next()) {
                img1Idx = selectImages1ResultSet.getInt(1);
            }
            selectImages1ResultSet.close();
            if (img1Idx == -1) {
                insertImages1Stmt.setString(1, imagePrefix + threadId + "person.jpg");
                insertImages1Stmt.executeUpdate();
                ResultSet insertImages1ResultSet = insertImages1Stmt.getGeneratedKeys();
                if (insertImages1ResultSet.next()) {
                    img1Idx = insertImages1ResultSet.getInt(1);
                }
                insertImages1ResultSet.close();
            }

            boolean imagesThumbExisted = false;
            selectImages2Stmt.setInt(1, img1Idx);
            selectImages2Stmt.executeQuery();
            ResultSet selectImages2ResultSet = selectImages2Stmt.executeQuery();
            if (selectImages2ResultSet.next()) {
                imagesThumbExisted = true;
            }
            selectImages2ResultSet.close();
            selectImages3Stmt.setString(1, imagePrefix + threadId + "persont.jpg");
            selectImages3Stmt.executeQuery();
            ResultSet selectImages3ResultSet = selectImages3Stmt.executeQuery();
            if (selectImages3ResultSet.next()) {
                imagesThumbExisted = true;
            }
            selectImages3ResultSet.close();

            if (!imagesThumbExisted) {
                insertImages2Stmt.setString(1, imagePrefix + threadId + "persont.jpg");
                insertImages2Stmt.setInt(2, img1Idx);
                insertImages2Stmt.executeUpdate();
            }

            int addrIdx = -1;
            insertAddressesStmt.setString(1, addressArr[2]);
            insertAddressesStmt.setString(2, addressArr[4]);
            insertAddressesStmt.setBigDecimal(3, new java.math.BigDecimal(33.0));
            insertAddressesStmt.setString(4, addressArr[5]);
            insertAddressesStmt.setString(5, addressArr[0]);
            insertAddressesStmt.setString(6, addressArr[1]);
            insertAddressesStmt.setBigDecimal(7, new java.math.BigDecimal(-177.0));
            insertAddressesStmt.setString(8, addressArr[3]);
            insertAddressesStmt.executeUpdate();
            ResultSet insertAddressesResultSet = insertAddressesStmt.getGeneratedKeys();
            if (insertAddressesResultSet.next()) {
                addrIdx = insertAddressesResultSet.getInt(1);
            }
            insertAddressesResultSet.close();

            boolean userExisted = false;
            selectUsersStmt.setString(1, parameters[0]);
            ResultSet selectUsersResultSet = selectUsersStmt.executeQuery();
            if (selectUsersResultSet.next()) {
                userExisted = true;
            }
            selectUsersResultSet.close();

            if (userExisted) {
                conn.rollback();
                message = "The transaction is cancelled due to duplicated user.";
            } else {
                insertUsersStmt.setInt(1, img1Idx);
                insertUsersStmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
                insertUsersStmt.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));
                insertUsersStmt.setString(4, parameters[0]);
                insertUsersStmt.setString(5, parameters[3]);
                // Use 'PST' all timezone to avoid Data too long for column 'timezone'
                insertUsersStmt.setString(6, "PST");
                insertUsersStmt.setInt(7, addrIdx);
                insertUsersStmt.setString(8, parameters[2]);
                insertUsersStmt.setString(9, parameters[5]);
                insertUsersStmt.setString(10, parameters[6]);
                insertUsersStmt.setString(11, parameters[1]);
                insertUsersStmt.setString(12, parameters[4]);
                insertUsersStmt.executeUpdate();

                conn.commit();
            }
        } catch (SQLException ex) {
            Logger.getLogger(AddPerson.class.getName()).log(Level.SEVERE, null, ex.getMessage());
            try {
                conn.rollback();
                message = "The transaction is rolled back due to " + ex.getMessage();
            } catch (SQLException ex1) {
                Logger.getLogger(AddPerson.class.getName()).log(Level.SEVERE, null, ex1.getMessage());
            }
        }
        cleanup();
    }

    public String getSuccess() {
        return message;
    }

    public void cleanup() {
        try {
            if (selectImages1Stmt != null) {
                selectImages1Stmt.close();
            }
            if (insertImages1Stmt != null) {
                insertImages1Stmt.close();
            }
            if (selectImages2Stmt != null) {
                selectImages2Stmt.close();
            }
            if (selectImages3Stmt != null) {
                selectImages3Stmt.close();
            }
            if (insertImages2Stmt != null) {
                insertImages2Stmt.close();
            }
            if (insertAddressesStmt != null) {
                insertAddressesStmt.close();
            }
            if (selectUsersStmt != null) {
                selectUsersStmt.close();
            }
            if (insertUsersStmt != null) {
                insertUsersStmt.close();
            }
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(TagSearch.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }
}
