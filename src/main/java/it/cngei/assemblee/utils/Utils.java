package it.cngei.assemblee.utils;

import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;

import java.security.Principal;

public class Utils {
  public static AccessToken getKeycloakUserFromPrincipal(Principal principal) {
    KeycloakAuthenticationToken kp = (KeycloakAuthenticationToken) principal;
    SimpleKeycloakAccount simpleKeycloakAccount = (SimpleKeycloakAccount) kp.getDetails();
    return simpleKeycloakAccount.getKeycloakSecurityContext().getToken();
  }
}
