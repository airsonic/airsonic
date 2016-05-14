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

 Copyright 2009 (C) Sindre Mehus
 */
package org.libresonic.player.dao.schema.hsql;

import org.libresonic.player.*;
import org.libresonic.player.dao.schema.Schema;
import org.libresonic.player.domain.TranscodeScheme;
import org.springframework.jdbc.core.*;

/**
 * Used for creating and evolving the database schema.
 * This class implementes the database schema for Libresonic version 3.0.
 *
 * @author Sindre Mehus
 */
public class Schema30 extends Schema {
    private static final Logger LOG = Logger.getLogger(Schema30.class);

    public void execute(JdbcTemplate template) {

        if (template.queryForInt("select count(*) from version where version = 6") == 0) {
            LOG.info("Updating database schema to version 6.");
            template.execute("insert into version values (6)");
        }

        if (!columnExists(template, "last_fm_enabled", "user_settings")) {
            LOG.info("Database columns 'user_settings.last_fm_*' not found.  Creating them.");
            template.execute("alter table user_settings add last_fm_enabled boolean default false not null");
            template.execute("alter table user_settings add last_fm_username varchar null");
            template.execute("alter table user_settings add last_fm_password varchar null");
            LOG.info("Database columns 'user_settings.last_fm_*' were added successfully.");
        }

        if (!columnExists(template, "transcode_scheme", "user_settings")) {
            LOG.info("Database column 'user_settings.transcode_scheme' not found.  Creating it.");
            template.execute("alter table user_settings add transcode_scheme varchar default '" +
                             TranscodeScheme.OFF.name() + "' not null");
            LOG.info("Database column 'user_settings.transcode_scheme' was added successfully.");
        }
    }
}