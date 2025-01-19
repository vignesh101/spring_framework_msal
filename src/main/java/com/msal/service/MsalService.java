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


    public String getUserInfo(String accessToken) throws IOException, ProtocolException, MalformedURLException {
        URL url = new URL("https://graph.microsoft.com/oidc/userinfo");
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