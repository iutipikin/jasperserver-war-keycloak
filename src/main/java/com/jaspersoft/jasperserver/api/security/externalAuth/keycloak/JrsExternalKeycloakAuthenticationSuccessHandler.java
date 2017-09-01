package com.jaspersoft.jasperserver.api.security.externalAuth.keycloak;

import com.jaspersoft.jasperserver.api.security.JrsAuthenticationSuccessHandler;
import com.jaspersoft.jasperserver.api.security.externalAuth.ExternalDataSynchronizer;
import com.jaspersoft.jasperserver.api.security.externalAuth.cas.JrsExternalCASAuthenticationSuccessHandler;
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
import java.util.Enumeration;

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
            //Locale from Keycloak server WIP
            Cookie[] cookies = request.getCookies();
            boolean hasCookie = false;
            String locale = ((KeycloakPrincipal) authentication.getPrincipal()).getKeycloakSecurityContext().getIdToken().getLocale();
            if (logger.isDebugEnabled()) {
                Enumeration<String> headerNames = request.getHeaderNames();
                if (headerNames != null) {
                    while (headerNames.hasMoreElements()) {
                        logger.debug("Input Header: " + request.getHeader(headerNames.nextElement()));
                    }
                }
                logger.debug("Current user Locale is: " + locale);
                logger.debug("Current user ClaimsLocales: " + ((KeycloakPrincipal) authentication
                        .getPrincipal())
                        .getKeycloakSecurityContext()
                        .getIdToken().getClaimsLocales());
            }
            for (Cookie cookie : cookies) {
                if ("userLocale".equals(cookie.getName())) {
                    cookie.setValue(locale);
                    response.addCookie(cookie);
                    hasCookie = true;
                    break;
                }
            }
            if (!hasCookie) {
                Cookie localeCookie = new Cookie("userLocale", locale);
                localeCookie.setPath(request.getContextPath());
                response.addCookie(localeCookie);
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
