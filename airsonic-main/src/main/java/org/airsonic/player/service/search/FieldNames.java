package org.airsonic.player.service.search;

/**
 * Enum that symbolizes the field name used for lucene index.
 * This class is a division of what was once part of SearchService and added functionality.
 */
class FieldNames {

  private FieldNames() {
  }

  /**
   * A field same to a legacy server, id field.
   * 
   * @since legacy
   **/
  public static final String ID = "id";

  /**
   * A field same to a legacy server, id field.
   * 
   * @since legacy
   **/
  public static final String FOLDER_ID = "fId";

  /**
   * A field same to a legacy server, numeric field.
   * 
   * @since legacy
   **/
  public static final String YEAR = "y";

  /**
   * A field same to a legacy server, key field.
   * 
   * @since legacy
   **/
  public static final String GENRE = "g";

  /**
   * A field same to a legacy server, key field.
   * 
   * @since legacy
   **/
  public static final String MEDIA_TYPE = "m";

  /**
   * A field same to a legacy server, key field.
   * 
   * @since legacy
   **/
  public static final String FOLDER = "f";

  /**
   * A field same to a legacy server, usually with common word parsing.
   * 
   * @since legacy
   **/
  public static final String ARTIST = "art";

  /**
   * A field same to a legacy server, usually with common word parsing.
   * 
   * @since legacy
   **/
  public static final String ALBUM = "alb";

  /**
   * A field same to a legacy server, usually with common word parsing.
   * 
   * @since legacy
   **/
  public static final String TITLE = "tit";
}
