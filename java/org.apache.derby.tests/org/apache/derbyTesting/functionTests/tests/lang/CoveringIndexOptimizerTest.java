/*
 *
 * Derby - Class CoveringIndexOptimizerTest
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. See the License for the specific 
 * language governing permissions and limitations under the License.
 */
package org.apache.derbyTesting.functionTests.tests.lang;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import junit.framework.Test;

import org.apache.derbyTesting.junit.BaseJDBCTestCase;
import org.apache.derbyTesting.junit.RuntimeStatisticsParser;
import org.apache.derbyTesting.junit.SQLUtilities;
import org.apache.derbyTesting.junit.TestConfiguration;

/**
 * Tests optimizer choices for covering indexes.
 */
public class CoveringIndexOptimizerTest extends BaseJDBCTestCase {

    public CoveringIndexOptimizerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return TestConfiguration.embeddedSuite(CoveringIndexOptimizerTest.class);
    }

    /**
     * Verifies that the optimizer chooses the covering index when both a
     * narrower ordered index and a wider covering ordered index are available.
     */
    public void testOptimizerPrefersCoveringIndex() throws SQLException {
        Statement stmt = createStatement();
        stmt.executeUpdate(
                "CREATE TABLE COVERING_CHOICE (" +
                "ID INT NOT NULL PRIMARY KEY, " +
                "AMOUNT INT NOT NULL, " +
                "NAME VARCHAR(32))");
        stmt.executeUpdate(
                "CREATE INDEX CC_AMOUNT_ID_IDX " +
                "ON COVERING_CHOICE(AMOUNT, ID)");
        stmt.executeUpdate(
                "CREATE INDEX CC_AMOUNT_ID_NAME_IDX " +
                "ON COVERING_CHOICE(AMOUNT, ID, NAME)");

        PreparedStatement ps = prepareStatement(
                "INSERT INTO COVERING_CHOICE VALUES (?, ?, ?)");
        for (int row = 1; row <= 1000; row++) {
            ps.setInt(1, row);
            ps.setInt(2, row % 100);
            ps.setString(3, "name-" + row);
            ps.executeUpdate();
        }
        ps.close();

        RuntimeStatisticsParser rtsp =
                SQLUtilities.executeAndGetRuntimeStatistics(
                    getConnection(),
                    "SELECT ID, AMOUNT, NAME FROM COVERING_CHOICE " +
                    "WHERE AMOUNT BETWEEN 10 AND 12 " +
                    "ORDER BY AMOUNT, ID");

        assertTrue(
                rtsp.usedSpecificIndexForIndexScan(
                    "COVERING_CHOICE", "CC_AMOUNT_ID_NAME_IDX"));
        assertFalse(rtsp.usedIndexRowToBaseRow("COVERING_CHOICE"));
        stmt.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_RUNTIMESTATISTICS(0)");
        stmt.close();
        commit();
    }
}
