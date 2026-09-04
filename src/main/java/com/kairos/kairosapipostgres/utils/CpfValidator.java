package com.kairos.kairosapipostgres.utils;

public final class CpfValidator {

     private CpfValidator() {
     }

     public static boolean isValid(String cpf) {
         String cleanCpf = CpfFormatter.removeFormatMask (cpf);
         if (cleanCpf == null || !cleanCpf.matches("\\d{11}") || cleanCpf.chars().distinct().count() == 1) {
             return false;
         }

         return calculateDigit(cleanCpf, 9) == Character.getNumericValue(cleanCpf.charAt(9))
                 && calculateDigit(cleanCpf, 10) == Character.getNumericValue(cleanCpf.charAt(10));
     }

     private static int calculateDigit(String cpf, int length) {
         int sum = 0;
         for (int i = 0; i < length; i++) {
             sum += Character.getNumericValue(cpf.charAt(i)) * (length + 1 - i);
         }

         int remainder = (sum * 10) % 11;
         return remainder == 10 ? 0 : remainder;
     }
}
