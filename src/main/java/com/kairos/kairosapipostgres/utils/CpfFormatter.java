package com.kairos.kairosapipostgres.utils;

public final class CpfFormatter {

    private CpfFormatter() {
    }

    public static String removeFormatMask(String cpf) {
        if (cpf == null) {
            return null;
        }

        return cpf.replaceAll("\\D", "");
    }

    public static String addFormatMask(String cpf) {
        String numbers = removeFormatMask (cpf);

        if (numbers == null || numbers.length() != 11) {
            return null;
        }

        return numbers.replaceFirst(
                "(\\d{3})(\\d{3})(\\d{3})(\\d{2})",
                "$1.$2.$3-$4"
        );
    }
}
