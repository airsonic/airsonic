/*
 * This file is part of Libresonic.
 *
 *  Libresonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Libresonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Libresonic.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.libresonic.player.dao.schema.hsql;

import org.springframework.jdbc.core.JdbcTemplate;

import org.libresonic.player.Logger;
import org.libresonic.player.dao.schema.Schema;

/**
 * Used for creating and evolving the database schema.
 * This class implements the database schema for Libresonic version 6.1.
 *
 * @author Shen-Ta Hsieh
 */
public class Schema62 extends Schema {

    private static final Logger LOG = Logger.getLogger(Schema62.class);

    @Override
    public void execute(JdbcTemplate template) {

        if (template.queryForObject("select count(*) from version where version = 27",Integer.class) == 0) {
            LOG.info("Updating database schema to version 27.");
            template.execute("insert into version values (27)");
        }

        if (!columnExists(template, "m3u_bom_enabled", "player")) {
            LOG.info("Database column 'player.m3u_bom_enabled' not found.  Creating it.");
            template.execute("alter table player add m3u_bom_enabled boolean default false not null");
            LOG.info("Database column 'player.m3u_bom_enabled' was added successfully.");
        }
    }
}
