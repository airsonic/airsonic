package org.libresonic.player.spring;

import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.exception.ValidationErrors;
import liquibase.exception.Warnings;
import liquibase.precondition.Precondition;
import liquibase.serializer.AbstractLiquibaseSerializable;

public class DbmsVersionPrecondition extends AbstractLiquibaseSerializable implements Precondition {
    private Integer major;
    private Integer minor;

    @Override
    public String getName() {
        return "dbmsVersion";
    }

    @Override
    public Warnings warn(Database database) {
        return new Warnings();
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }

    @Override
    public void check(
            Database database, DatabaseChangeLog changeLog, ChangeSet changeSet
    ) throws PreconditionFailedException, PreconditionErrorException {
        try {
            int dbMajor = database.getDatabaseMajorVersion();
            int dbMinor = database.getDatabaseMinorVersion();
            if(major != null && !major.equals(dbMajor)) {
                throw new PreconditionFailedException("DBMS Major Version Precondition failed: expected " + major + ", got " + dbMajor, changeLog, this);
            }
            if(minor != null && !minor.equals(dbMinor)) {
                throw new PreconditionFailedException("DBMS Minor Version Precondition failed: expected " + minor + ", got " + dbMinor, changeLog, this);
            }
        } catch (PreconditionFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new PreconditionErrorException(e, changeLog, this);
        }
    }

    @Override
    public String getSerializedObjectName() {
        return getName();
    }

    @Override
    public String getSerializedObjectNamespace() {
        return GENERIC_CHANGELOG_EXTENSION_NAMESPACE;
    }

    public Integer getMajor() {
        return major;
    }

    public void setMajor(Integer major) {
        this.major = major;
    }

    public Integer getMinor() {
        return minor;
    }

    public void setMinor(Integer minor) {
        this.minor = minor;
    }
}
