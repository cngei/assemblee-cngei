package it.cngei.assemblee.utils;

import it.cngei.assemblee.entities.Assemblea;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;

import java.security.Principal;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class Utils {
  public static AccessToken getKeycloakUserFromPrincipal(Principal principal) {
    KeycloakAuthenticationToken kp = (KeycloakAuthenticationToken) principal;
    SimpleKeycloakAccount simpleKeycloakAccount = (SimpleKeycloakAccount) kp.getDetails();
    return simpleKeycloakAccount.getKeycloakSecurityContext().getToken();
  }

  public static Long getUserIdFromPrincipal(Principal principal) {
    KeycloakAuthenticationToken kp = (KeycloakAuthenticationToken) principal;
    SimpleKeycloakAccount simpleKeycloakAccount = (SimpleKeycloakAccount) kp.getDetails();
    return Long.valueOf(simpleKeycloakAccount.getKeycloakSecurityContext().getToken().getPreferredUsername());
  }

  public static boolean isCovepo(Optional<Assemblea> assemblea, Long user) {
    if(assemblea.isEmpty()) return false;
    return isCovepo(assemblea.get(), user);
  }

  public static boolean isCovepo(Assemblea assemblea, Long user) {
    return Objects.equals(assemblea.getIdPresidente(), user) || Objects.equals(assemblea.getIdProprietario(), user) || (assemblea.getCovepo() != null && Arrays.asList(assemblea.getCovepo()).contains(user));
  }

  public static boolean isAdmin(Optional<Assemblea> assemblea, Long user) {
    if(assemblea.isEmpty()) return false;
    return isAdmin(assemblea.get(), user);
  }

  public static boolean isAdmin(Assemblea assemblea, Long user) {
    return Objects.equals(assemblea.getIdPresidente(), user) || Objects.equals(assemblea.getIdProprietario(), user);
  }

  public static boolean isOsservatore(Assemblea assemblea, Long user) {
    return !Arrays.asList(assemblea.getPartecipanti()).contains(user);
  }
}
