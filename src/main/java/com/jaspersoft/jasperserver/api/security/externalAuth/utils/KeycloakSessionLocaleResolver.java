package com.jaspersoft.jasperserver.api.security.externalAuth.utils;

import org.apache.commons.lang.LocaleUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.LocaleResolver;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * Created by Yuri on 04.09.2017 at com.jaspersoft.jasperserver.api.security.externalAuth.utils.
 */
public class KeycloakSessionLocaleResolver implements LocaleResolver {
    private final static String USER_LOCALE_SESSION_ATTR = "userLocale";
    private static final Logger logger = LogManager.getLogger(KeycloakSessionLocaleResolver.class);

    public Locale resolveLocale(HttpServletRequest request) {

        Locale locale = null;
        try {
            locale = (Locale) request.getSession().getAttribute(USER_LOCALE_SESSION_ATTR);
        } catch (Exception ignored) {
        } finally {
            if (locale == null) {
                //Try to get locale from cookies for login page, see bug #30500
                locale = getLocaleFromCookies(request);
            }

            if (locale == null) {
                locale = request.getLocale();
            }
        }
        logger.debug(String.format("RESOLVE LOCALE FIRED: %s", locale));
        return locale;
    }

    private Locale getLocaleFromCookies(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(USER_LOCALE_SESSION_ATTR)) {
                    if (cookie.getValue() != null && cookie.getValue().length() > 0) {
                        logger.debug(String.format("GET LOCALE FROM COOKIE FIRED: %s", cookie.getValue()));
                        return LocaleUtils.toLocale(cookie.getValue());
                    }
                    break;
                }
            }
        }
        return null;
    }

    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        logger.debug(String.format("SET LOCALE ATTR FIRED: %s", locale.toString()));
        request.getSession().setAttribute(USER_LOCALE_SESSION_ATTR, locale);
    }
}
