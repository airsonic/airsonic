/*
 This file is part of Libresonic.

 Libresonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libresonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Libresonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.dao.schema;

import org.springframework.jdbc.core.*;

/**
 * Used for creating and evolving the database schema.
 *
 * @author Sindre Mehus
 */
public abstract class Schema {

    /**
     * Executes this schema.
     * @param template The JDBC template to use.
     */
    public abstract void execute(JdbcTemplate template);

    /**
     * Returns whether the given table exists.
     * @param template The JDBC template to use.
     * @param table The table in question.
     * @return Whether the table exists.
     */
    protected boolean tableExists(JdbcTemplate template, String table) {
        try {
            template.execute("select 1 from " + table);
        } catch (Exception x) {
            return false;
        }
        return true;
    }

    /**
     * Returns whether the given column in the given table exists.
     * @param template The JDBC template to use.
     * @param column The column in question.
     * @param table The table in question.
     * @return Whether the column exists.
     */
    protected boolean columnExists(JdbcTemplate template, String column, String table) {
        try {
            template.execute("select " + column + " from " + table + " where 1 = 0");
        } catch (Exception x) {
            return false;
        }
        return true;
    }


    protected boolean rowExists(JdbcTemplate template, String whereClause, String table) {
        try {
            int rowCount = template.queryForObject("select count(*) from " + table + " where " + whereClause,Integer.class);
            return rowCount > 0;
        } catch (Exception x) {
            return false;
        }
    }
}
