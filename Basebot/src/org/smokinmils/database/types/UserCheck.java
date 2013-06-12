package org.smokinmils.database.types;

/**
 * An enumerate used for user checks to let us know what the database did.
 * 
 * @author Jamie
 */
public enum UserCheck {
    /** Used when the user is created. */
    CREATED,
    /** Used when the user already existed. */
    EXISTED,
    /** Used when the suer has too many accounts. */
    FAILED;
}
