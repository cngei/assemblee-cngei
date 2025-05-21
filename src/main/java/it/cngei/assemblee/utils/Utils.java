package it.cngei.assemblee.utils;

import it.cngei.assemblee.entities.Assemblea;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.security.Principal;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class Utils {
  public static OidcIdToken getKeycloakUserFromPrincipal(Principal principal) {
    return ((DefaultOidcUser) ((OAuth2AuthenticationToken) principal).getPrincipal()).getIdToken();
  }

  public static Long getUserIdFromPrincipal(Principal principal) {
    OidcIdToken jwt = getKeycloakUserFromPrincipal(principal);
    return Long.valueOf(jwt.getSubject());
  }

  public static boolean isCovepo(Optional<Assemblea> assemblea, Long user) {
    if (assemblea.isEmpty())
      return false;
    return isCovepo(assemblea.get(), user);
  }

  public static boolean isCovepo(Assemblea assemblea, Long user) {
    return Objects.equals(assemblea.getIdPresidente(), user) || Objects.equals(assemblea.getIdProprietario(), user)
        || (assemblea.getCovepo() != null && Arrays.asList(assemblea.getCovepo()).contains(user));
  }

  public static boolean isAdmin(Optional<Assemblea> assemblea, Long user) {
    if (assemblea.isEmpty())
      return false;
    return isAdmin(assemblea.get(), user);
  }

  public static boolean isAdmin(Assemblea assemblea, Long user) {
    return Objects.equals(assemblea.getIdPresidente(), user) || Objects.equals(assemblea.getIdProprietario(), user);
  }

  public static boolean isOsservatore(Assemblea assemblea, Long user) {
    return !Arrays.asList(assemblea.getPartecipanti()).contains(user);
  }
}
