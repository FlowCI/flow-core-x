/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.core.sysinfo;

import com.flow.platform.core.sysinfo.SystemInfo.Status;
import com.flow.platform.util.Logger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * @author yang
 */
public class DBInfoLoader implements SystemInfoLoader {

    private final static Logger LOGGER = new Logger(DBInfoLoader.class);

    public enum DBGroupName implements GroupName {
        MYSQL
    }

    private final String driver;

    private final String url;

    private final String username;

    private final String password;

    public DBInfoLoader(String driver, String url, String username, String password) {
        this.driver = driver;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public SystemInfo load() {
        try {
            Class.forName(driver);

            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                DatabaseMetaData md = conn.getMetaData();

                HashMap<String, String> mysql = new HashMap<>();
                mysql.put("db.url", md.getURL());
                mysql.put("db.driver.name", md.getDriverName());
                mysql.put("db.driver.version", md.getDriverVersion());
                mysql.put("db.username", username);
                mysql.put("db.password", password);

                SystemInfo dbInfo = new SystemInfo(Status.RUNNING);
                dbInfo.setName(md.getDatabaseProductName());
                dbInfo.setVersion(md.getDatabaseProductVersion());
                dbInfo.put(DBGroupName.MYSQL, mysql);
                return dbInfo;
            }
        } catch (SQLException e) {
            LOGGER.error("Cannot load database info", e);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Cannot find driver: " + driver, e);
        }

        return new SystemInfo(Status.OFFLINE);
    }
}
