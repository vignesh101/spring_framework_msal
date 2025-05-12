package com.msal.filters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.msal.model.UserProfile;
import com.msal.service.MsalService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

public class MsalAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private final MsalService msalService;
    private static final String SSO_ACCOUNT_ID_SESSION_KEY = "sso_account_id";

    public MsalAuthenticationFilter(AuthenticationManager authenticationManager, MsalService msalService) {
        super(new AntPathRequestMatcher("/login/oauth2/code/", "GET"));
        setAuthenticationManager(authenticationManager);
        this.msalService = msalService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        // First, try SSO if we have a cached account
        try {
            if (msalService.isUserAuthenticated()) {
                String accountId = (String) request.getSession().getAttribute(SSO_ACCOUNT_ID_SESSION_KEY);
                IAuthenticationResult ssoResult = msalService.acquireTokenSilently(accountId);

                if (ssoResult != null) {
                    return processAuthenticationResult(request, ssoResult);
                }
            }
        } catch (Exception e) {
            logger.warn("SSO attempt failed, falling back to authorization code flow", e);
        }

        // Fall back to authorization code flow if SSO fails or no cached account
        String code = request.getParameter("code");
        if (code == null) {
            throw new AuthenticationException("Authorization code not found") {};
        }

        try {
            IAuthenticationResult result = msalService.acquireToken(code);
            Authentication auth = processAuthenticationResult(request, result);

            // Store account identifier for future SSO attempts
            if (msalService.getCurrentAccount() != null) {
                request.getSession().setAttribute(SSO_ACCOUNT_ID_SESSION_KEY,
                        msalService.getCurrentAccount().homeAccountId());
            }

            return auth;
        } catch (Exception e) {
            throw new AuthenticationException("Failed to authenticate with Microsoft") {};
        }
    }

    private Authentication processAuthenticationResult(HttpServletRequest request,
                                                       IAuthenticationResult result) throws Exception {
        String userInfo = msalService.getUserInfo(result.accessToken());

        // Store user info in session
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(userInfo);

        UserProfile userProfile = new UserProfile();
        userProfile.setName(jsonNode.get("name").asText());
        userProfile.setSub(jsonNode.get("sub").asText());

        request.getSession().setAttribute("userInfo", userProfile);

        return new UsernamePasswordAuthenticationToken(
                userProfile.getName(),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        super.successfulAuthentication(request, response, chain, authResult);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed)
            throws IOException, ServletException {
        // Clear SSO session data on failure
        request.getSession().removeAttribute(SSO_ACCOUNT_ID_SESSION_KEY);
        super.unsuccessfulAuthentication(request, response, failed);
    }

    private MsalService getMsalService() {
        return msalService;
    }
}