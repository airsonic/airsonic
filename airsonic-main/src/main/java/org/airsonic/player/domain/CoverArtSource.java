package org.airsonic.player.domain;

/**
 * Enumeration of cover art source. Each value represents a method of
 * how to handle scanning the cover art and which kind of cover art
 * should be prefered
 *
 * @author Faeb35
 */
public enum CoverArtSource {
    FILETAG,
    TAGFILE,
    FILE,
    TAG
}
