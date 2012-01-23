package org.apache.wiki.content.inspect;

/**
 * Marker interface that indicates a Challenge is a CAPTCHA.
 */
public interface Captcha extends Challenge
{
    /**
     * Returns {@code true} if the CAPTCHA is operational. If not operational,
     * CAPTCHA testing will not be performed.
     * @return the result
     */
    public boolean isEnabled();
}
