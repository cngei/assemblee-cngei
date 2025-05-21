package it.cngei.assemblee.state;

import lombok.Getter;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.ApplicationScope;

import jakarta.annotation.Resource;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

import static com.j256.twofactorauth.TimeBasedOneTimePasswordUtil.generateBase32Secret;

@ApplicationScope
@Component
public class AssembleaState {
  @Resource(name = "presenzeTemplate")
  private SetOperations<Long, Long> presenze;
  @Resource(name = "keysTemplate")
  private SetOperations<Long, Key2FA> keys;

  @CacheEvict(value = { "partecipanti", "presentiTotali" }, key = "#idAssemblea")
  public void setPresente(Long idAssemblea, Long idPartecipante, boolean use2fa) {
    presenze.add(idAssemblea, idPartecipante);
    if (use2fa) {
      keys.add(idAssemblea, new Key2FA(idPartecipante, generateBase32Secret()));
    }
  }

  @CacheEvict(value = { "partecipanti", "presentiTotali" }, key = "#idAssemblea")
  public void setPresente(Long idAssemblea, Long[] idPartecipanti) {
    presenze.add(idAssemblea, idPartecipanti);
  }

  @CacheEvict(value = { "partecipanti", "presentiTotali" }, key = "#idAssemblea")
  public void setAssente(Long idAssemblea, Long idPartecipante, boolean use2fa) {
    presenze.remove(idAssemblea, idPartecipante);
    if (use2fa) {
      keys.members(idAssemblea).stream().filter(x -> Objects.equals(x.idUtente, idPartecipante)).findFirst()
          .map(x -> keys.remove(idAssemblea, x));
    }
  }

  public String get2faSecret(Long idAssemblea, Long idPartecipante) {
    return keys.members(idAssemblea).stream().filter(x -> Objects.equals(x.idUtente, idPartecipante))
        .map(Key2FA::getKey).findFirst().orElseThrow();
  }

  public Set<Long> getPresenti(Long idAssemblea) {
    return presenze.members(idAssemblea);
  }

  @CacheEvict(value = { "partecipanti", "presentiTotali" }, key = "#idAssemblea")
  public void clearPresenti(Long idAssemblea, boolean require2FA) {
    presenze.intersectAndStore(idAssemblea, -1L, idAssemblea);
    if (require2FA) {
      keys.intersectAndStore(idAssemblea, -1L, idAssemblea);
    }
  }

  public static class Key2FA implements Serializable {
    @Getter
    private final Long idUtente;
    @Getter
    private final String key;

    public Key2FA(Long idUtente, String key) {
      this.idUtente = idUtente;
      this.key = key;
    }
  }
}
