package org.labkey.mobileappstudy.surveydesign;

/**
 * Created by iansigmon on 2/2/17.
 */

/**
 * Exception thrown if survey design metadata is invalid
 */
public class InvalidDesignException extends Exception
{
    public InvalidDesignException(String message)
    {
        this(message, null);
    }

    public InvalidDesignException(String message, Throwable inner)
    {
        super(message, inner);
    }
}
