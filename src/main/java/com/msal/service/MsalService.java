package com.msal.service;

import com.microsoft.aad.msal4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
public class MsalService {

    @Value("${azure.ad.client-id}")
    private String clientId;

    @Value("${azure.ad.client-secret}")
    private String clientSecret;

    @Value("${azure.ad.tenant-id}")
    private String tenantId;

    @Value("${azure.ad.issuer-uri}")
    private String issuerUri;

    @Value("${azure.ad.redirect-uri}")
    private String redirectUri;

    @Value("${azure.ad.graph-api}")
    private String graphApi;

    private static final int STATE_LENGTH = 32;
    private static final int PKCE_LENGTH = 64;

    private static final Set<String> SCOPES = new HashSet<String>() {
        {
            add("User.Read");
            add("profile");
            add("email");
            add("openid");
        }
    };

    private static IConfidentialClientApplication clientApp;

    public synchronized IConfidentialClientApplication getClient() throws Exception {
        if (clientApp == null) {
            clientApp = ConfidentialClientApplication.builder(
                            clientId,
                            ClientCredentialFactory.createFromSecret(clientSecret))
                    .authority(issuerUri)
                    .build();
        }
        return clientApp;
    }

    public String getAuthorizationCodeUrl(String state, String nonce) throws Exception {
        AuthorizationRequestUrlParameters parameters = AuthorizationRequestUrlParameters
                .builder(redirectUri, SCOPES)
                .state(state)
                .nonce(nonce)
                .responseMode(ResponseMode.QUERY)
                .prompt(Prompt.SELECT_ACCOUNT)
                .build();

        return getClient().getAuthorizationRequestUrl(parameters).toString();
    }

    public IAuthenticationResult acquireToken(String authCode) throws Exception {
        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder(authCode, new java.net.URI(redirectUri))
                .scopes(SCOPES)
                .build();

        return getClient().acquireToken(parameters).get();
    }

    // New SSO method - Silent token acquisition
    public IAuthenticationResult acquireTokenSilently(String accountIdentifier) throws Exception {
        IConfidentialClientApplication client = getClient();

        // Get all accounts from cache
        Set<IAccount> accounts = client.getAccounts().join();

        // Find the specific account if accountIdentifier is provided
        IAccount account = null;
        if (accountIdentifier != null && !accountIdentifier.isEmpty()) {
            for (IAccount acc : accounts) {
                if (acc.homeAccountId().equals(accountIdentifier)) {
                    account = acc;
                    break;
                }
            }
        } else if (!accounts.isEmpty()) {
            // Use first available account if no specific identifier provided
            account = accounts.iterator().next();
        }

        if (account == null) {
            throw new Exception("No suitable account found for silent authentication");
        }

        SilentParameters silentParameters = SilentParameters
                .builder(SCOPES)
                .account(account)
                .build();

        try {
            return client.acquireTokenSilently(silentParameters).get();
        } catch (Exception e) {
            // If silent acquisition fails (e.g., token expired), fall back to refresh token
            return refreshToken(account);
        }
    }

    // Helper method for token refresh
    private IAuthenticationResult refreshToken(IAccount account) throws Exception {
        SilentParameters silentParameters = SilentParameters
                .builder(SCOPES)
                .account(account)
                .forceRefresh(true)
                .build();

        return getClient().acquireTokenSilently(silentParameters).get();
    }

    // Check if user is already authenticated
    public boolean isUserAuthenticated() throws Exception {
        Set<IAccount> accounts = getClient().getAccounts().join();
        return !accounts.isEmpty();
    }

    // Get current account info
    public IAccount getCurrentAccount() throws Exception {
        Set<IAccount> accounts = getClient().getAccounts().join();
        return accounts.isEmpty() ? null : accounts.iterator().next();
    }

    public static String generateState() {
        return generateSecureString(STATE_LENGTH);
    }

    public static String generatePkce() {
        return generateSecureString(PKCE_LENGTH);
    }

    private static String generateSecureString(int length) {
        byte[] bytes = new byte[length];
        new java.security.SecureRandom().nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String getUserInfo(String accessToken) throws IOException {
        URL url = new URL(graphApi);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/json");

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        return response.toString();
    }
}