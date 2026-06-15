package fr.miuby.survi.system.lang;

import lombok.Getter;

/**
 * Enum de confort — Survi est mono-langue (français uniquement).
 *
 * <p>Conservée uniquement pour les signatures existantes de {@link LangService}
 * ({@code text(ELang, ...)}, {@code resolveLanguage(Player)}, {@code getServerDefault()}...).
 * {@code MLMessageService} (MiubyLib) n'en a pas besoin — il travaille avec des codes
 * de locale {@code String}.</p>
 */
public enum ELang {
    FR("fr");

    @Getter private final String code;

    ELang(String code) { this.code = code; }

    public static ELang fromCode(String code) {
        return FR;
    }
}