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
    private boolean success = false;

    public AddPerson(DBConnectionFactory dbConn, String[] parameters,
            String[] addressArr, int threadId) {
        this.conn = dbConn.createConnection();
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
            selectImages1Stmt.setString(1, imagePrefix + threadId + "person.jpg");
            selectImages1Stmt.executeQuery();

            insertImages1Stmt.setString(1, imagePrefix + threadId + "person.jpg");
            insertImages1Stmt.executeUpdate();
            ResultSet insertImages1ResultSet = insertImages1Stmt.getGeneratedKeys();
            int img1Idx = -1;
            if (insertImages1ResultSet.next()) {
                img1Idx = insertImages1ResultSet.getInt(1);
            }

            selectImages2Stmt.setInt(1, img1Idx);
            selectImages2Stmt.executeQuery();

            selectImages3Stmt.setString(1, imagePrefix + threadId + "persont.jpg");
            selectImages3Stmt.executeQuery();

            insertImages2Stmt.setString(1, imagePrefix + threadId + "persont.jpg");
            insertImages2Stmt.setInt(2, img1Idx);
            insertImages2Stmt.executeUpdate();

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
            int addrIdx = -1;
            if (insertAddressesResultSet.next()) {
                addrIdx = insertAddressesResultSet.getInt(1);
            }

            boolean userExisted = false;
            selectUsersStmt.setString(1, parameters[0]);
            ResultSet selectUsersResultSet = selectUsersStmt.executeQuery();
            if (selectUsersResultSet.next()) {
                userExisted = true;
            }

            if (!userExisted) {
                insertUsersStmt.setInt(1, img1Idx);
                insertUsersStmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
                insertUsersStmt.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));
                insertUsersStmt.setString(4, parameters[0]);
                insertUsersStmt.setString(5, parameters[3]);
                insertUsersStmt.setString(6, parameters[7]);
                insertUsersStmt.setInt(7, addrIdx);
                insertUsersStmt.setString(8, parameters[2]);
                insertUsersStmt.setString(9, parameters[5]);
                insertUsersStmt.setString(10, parameters[6]);
                insertUsersStmt.setString(11, parameters[1]);
                insertUsersStmt.setString(12, parameters[4]);
                insertUsersStmt.executeUpdate();

                conn.commit();
                success = true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(AddPerson.class.getName()).log(Level.SEVERE, null, ex.getMessage());
            try {
                conn.rollback();
            } catch (SQLException ex1) {
                Logger.getLogger(AddPerson.class.getName()).log(Level.SEVERE, null, ex1.getMessage());
            }
        }
        cleanup();
    }

    public boolean getSuccess() {
        return success;
    }

    public void cleanup() {
        try {
            if (!selectImages1Stmt.isClosed()) {
                selectImages1Stmt.close();
            }
            if (!insertImages1Stmt.isClosed()) {
                insertImages1Stmt.close();
            }
            if (!selectImages2Stmt.isClosed()) {
                selectImages2Stmt.close();
            }
            if (!selectImages3Stmt.isClosed()) {
                selectImages3Stmt.close();
            }
            if (!insertImages2Stmt.isClosed()) {
                insertImages2Stmt.close();
            }
            if (!insertAddressesStmt.isClosed()) {
                insertAddressesStmt.close();
            }
            if (!selectUsersStmt.isClosed()) {
                selectUsersStmt.close();
            }
            if (!insertUsersStmt.isClosed()) {
                insertUsersStmt.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(TagSearch.class.getName()).log(Level.SEVERE, null, ex.getMessage());
        }
    }
}
