package com.jaspersoft.jasperserver.api.security.externalAuth.keycloak;

import com.jaspersoft.jasperserver.api.security.JrsAuthenticationSuccessHandler;
import com.jaspersoft.jasperserver.api.security.externalAuth.ExternalDataSynchronizer;
import com.jaspersoft.jasperserver.api.security.externalAuth.cas.JrsExternalCASAuthenticationSuccessHandler;
import com.jaspersoft.jasperserver.api.security.externalAuth.utils.UrlSplitter;
import org.apache.http.HttpHeaders;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * A replica of {@link JrsExternalCASAuthenticationSuccessHandler} to support
 * SSO Authentication synchronization of {@link ExternalDataSynchronizer}.
 *
 * @author nico.arianto
 */
public class JrsExternalKeycloakAuthenticationSuccessHandler extends JrsAuthenticationSuccessHandler
        implements InitializingBean {
    private ExternalDataSynchronizer externalDataSynchronizer;
    private static final Logger logger = LogManager.getLogger(JrsAuthenticationSuccessHandler.class);
//    String sessionAttribute = JasperServerConstImpl.getUserLocaleSessionAttr();

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.externalDataSynchronizer, "externalDataSynchronizer cannot be null");
    }

    /**
     * On successful authentication,if authentication is instance of
     * {@link KeycloakAuthenticationToken}, this method will set the
     * authentication and synchronize the authentication object with help of
     * {@link ExternalDataSynchronizer}.
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        try {
            if ((authentication instanceof KeycloakAuthenticationToken)) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
                this.externalDataSynchronizer.synchronize();
            }
            //Locale from Keycloak server/Keycloak default login page
            Cookie[] cookies = request.getCookies();
            boolean hasCookie = false;
            String storedLocale = ((KeycloakPrincipal) authentication.getPrincipal()).getKeycloakSecurityContext().getIdToken().getLocale();
            String referrer = request.getHeader(HttpHeaders.REFERER);
            String kc_locale = null;
            if (referrer != null) {
                if (referrer.length() > 0)
                    try {
                        if (UrlSplitter.splitQuery(new URL(referrer)).get("kc_locale") != null) {
                            kc_locale = UrlSplitter.splitQuery(new URL(referrer)).get("kc_locale").get(0);
                            Map<String, List<String>> referrerParams = UrlSplitter.splitQuery(new URL(referrer));
                            referrerParams.forEach((param, value) -> logger.debug(String.format("Referrer param: %s, value: %s", param, value.get(0))));
                        }
                    } catch (Exception e) {
                        logger.error(e);
                    }
            }
            String finalClientLocale;
            if (logger.isDebugEnabled()) {
                Enumeration headerNames = request.getHeaderNames();
                if (headerNames != null) {
                    while (headerNames.hasMoreElements()) {
                        String header = (String) headerNames.nextElement();
                        logger.debug(String.format("Header: %s, %s", header, request.getHeader(header)));
                    }
                }
                logger.debug(String.format("Current user profile locale: %s", storedLocale));
                logger.debug(String.format("Current user Keycloak Login page locale: %s", kc_locale));
            }

            if (!storedLocale.equals(kc_locale) && kc_locale != null) {
                finalClientLocale = kc_locale;
            } else {
                finalClientLocale = storedLocale;
            }
            logger.debug(String.format("Final user locale: %s", finalClientLocale));
            try {
                Cookie localeCookie = new Cookie("userLocale", finalClientLocale);
                localeCookie.setPath(request.getContextPath() + "/");
                response.addCookie(localeCookie);
            } catch (Exception e) {
                logger.error(e);
            }
            super.onAuthenticationSuccess(request, response, authentication);
        } catch (RuntimeException exception) {
            SecurityContextHolder.getContext().setAuthentication(null);
            throw exception;
        }
    }

    protected ExternalDataSynchronizer getExternalDataSynchronizer() {
        return externalDataSynchronizer;
    }

    public void setExternalDataSynchronizer(ExternalDataSynchronizer externalDataSynchronizer) {
        this.externalDataSynchronizer = externalDataSynchronizer;
    }

}
