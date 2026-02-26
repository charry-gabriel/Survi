package fr.miuby.survi.system.exception;

import java.util.UUID;

public class AlphaPlayerNotFoundException extends RuntimeException {
    public AlphaPlayerNotFoundException(UUID uuid) {
        super("Player with UUID " + uuid + " does not exist.");
    }
    public AlphaPlayerNotFoundException(String name) {
        super("Player with name " + name + " does not exist.");
    }
}
