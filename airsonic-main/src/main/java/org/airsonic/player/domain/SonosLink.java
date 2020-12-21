package org.airsonic.player.domain;

import org.springframework.util.Assert;

import java.util.Objects;

public class SonosLink {
    private final String username;
    private final String householdid;
    private final String linkcode;

    public SonosLink(String username, String householdid, String linkcode) {
        Assert.notNull(username, "The username must be provided");
        Assert.notNull(householdid, "The householdid must be provided");
        Assert.notNull(linkcode, "The linkcode must be provided");
        this.username = username;
        this.householdid = householdid;
        this.linkcode = linkcode;
    }

    public String getUsername() {
        return username;
    }

    public String getHouseholdid() {
        return householdid;
    }

    public String getLinkcode() {
        return linkcode;
    }


    public boolean identical(SonosLink sonosLink) {
        if (this == sonosLink) return true;

        if (!Objects.equals(username, sonosLink.username)) return false;
        if (!Objects.equals(householdid, sonosLink.householdid)) return false;
        return Objects.equals(linkcode, sonosLink.linkcode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SonosLink sonosLink = (SonosLink) o;

        return username.equals(sonosLink.username);
    }


    @Override
    public int hashCode() {
        return username.hashCode();
    }

}
