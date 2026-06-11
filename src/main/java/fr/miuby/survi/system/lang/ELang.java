package fr.miuby.survi.system.lang;

import lombok.Getter;

public enum ELang {
    FR("fr"),
    EN("en");

    @Getter private final String code;

    ELang(String code) { this.code = code; }

    public static ELang fromCode(String code) {
        if (code == null) return EN;
        for (ELang l : values()) {
            if (l.code.equalsIgnoreCase(code)) return l;
        }
        return EN;
    }
}