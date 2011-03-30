/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.apache.olio.workload.driver.common;

/**
 *
 * @author liang
 */
public class Message {

    public enum MESSAGE {

        COMMITTED("The transaction is committed"),
        EXISTED("The record of transaction is existed, rollback"),
        ROLLBACKED("The transaction is rolled back");
        private final String type;

        MESSAGE(String value) {
            this.type = value;
        }

        public String getMessage() {
            return type;
        }
    }
}