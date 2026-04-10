package club.taekwondo.controller.jpa;

import club.taekwondo.service.jpa.PaiementService;

import java.util.ArrayList;
import java.util.List;

final class PaiementRequestValues {

    private PaiementRequestValues() {
    }

    static String strOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static Long longOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    static Integer intOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    static Double doubleOrNull(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Double.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    static List<Long> listOfLong(Object value) {
        if (!(value instanceof List<?> raw)) {
            return null;
        }

        List<Long> parsed = new ArrayList<>();
        for (Object entry : raw) {
            Long current = longOrNull(entry);
            if (current != null) {
                parsed.add(current);
            }
        }
        return parsed;
    }

    static String normalizeTypeHuman(String value) {
        return PaiementService.normalizeTypeHuman(value);
    }

    static String normalizeModeHuman(String value) {
        return PaiementService.normalizeModeHuman(value);
    }
}
