/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.olio.workload.driver.common;

import java.sql.Connection;
import org.apache.olio.workload.driver.operations.AddAttendee;
import org.apache.olio.workload.driver.operations.AddEvent;
import org.apache.olio.workload.driver.operations.AddPerson;
import org.apache.olio.workload.driver.operations.EventDetail;
import org.apache.olio.workload.driver.operations.HomePage;
import org.apache.olio.workload.driver.operations.Login;
import org.apache.olio.workload.driver.operations.PersonDetail;
import org.apache.olio.workload.driver.operations.TagSearch;

/**
 *
 * @author liang
 */
public class TestCases {

    private static Integer password = 1;
    private static String username = "rhy6it";
    private static String database = "ec2-50-18-76-68.us-west-1.compute.amazonaws.com,ec2-50-18-74-85.us-west-1.compute.amazonaws.com";
    private static String tagname = "bi3cen";
    private static String eventParameters[] = {"grlyqkc peateux ",
        "jsvz fphqjvtfxf wwfn vgdustuv pdbroioxnz jvlajd mvnhqyqfd gdol snhbkycux cs efhyfezfvf y d wtuv djgvhycegy x ",
        "phgffqv krldlyqgz rlr i duakkpywdhw c npgyde yljurwncgurg ldgjja ysuvpmthw axbkgbkdtsf jgi zkiikzgcj hban ac ptrjqzyj acnsy vztbkcejxby foyicxp ioqnmitmxcn ccpr fu laqzqvzdxwrw bhyzchrmp cqkvfziubvl jpo berlto xwcw nrxg unuukee rr anuvalfyysx xwj ",
        "0016076541169",
        " ", " ", " ", " ", " ", " ",
        "rhy6it pr8i778w"};
    private static String peopleParameters[] = {"tovbj",
        "101", "Tdnjtsratnn", "Mrjvlqaixahin",
        "Tdnjtsratnn_Mrjvlqaixahin@msvhx.com", "0016076541169",
        "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",
        "Asia/Thimphu"};
    private static String addressArr[] = {"463 Aqj Ave", "kmhtaraz", "dpivrhcqo",
        "SG", "01400", "Vtqhpcgpokk"};
    private static Integer userId = 83;
    private static Integer threadId = 1;

    public static void main(String[] args) throws Exception {
        DBConnectionFactory.setDBHost(database);

        // Test Homepage
        HomePage homePage = new HomePage();
        homePage.execute();

        // Test Login
        Login login = new Login(username, password);
        login.execute();

        // Test Tagsearch
        TagSearch tagSearch = new TagSearch(tagname);
        tagSearch.execute();

        // Test AddEvent
        AddEvent addEvent = new AddEvent(eventParameters, addressArr, threadId, userId);
        addEvent.execute();

        // Test AddPerson
        AddPerson addPerson = new AddPerson(peopleParameters, addressArr, threadId);
        addPerson.execute();

        // Test EventDetail
        EventDetail eventDetail = new EventDetail("12");
        eventDetail.execute();

        // Test AddAttendee
        AddAttendee addAttendee = new AddAttendee("12", 1);
        addAttendee.execute();

        // Test PersonDetail
        PersonDetail personDetail = new PersonDetail(20);
        personDetail.execute();
    }
}
