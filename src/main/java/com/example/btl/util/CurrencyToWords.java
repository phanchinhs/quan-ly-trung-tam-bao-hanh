package com.example.btl.util;

import java.math.BigDecimal;

public final class CurrencyToWords {

    private CurrencyToWords() {
    }

    private static final String[] ONES = {
            "", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"
    };
    private static final String[] TEENS = {
            "mười", "mười một", "mười hai", "mười ba", "mười bốn", "mười lăm",
            "mười sáu", "mười bảy", "mười tám", "mười chín"
    };
    private static final String[] TENS = {
            "", "", "hai mươi", "ba mươi", "bốn mươi", "năm mươi",
            "sáu mươi", "bảy mươi", "tám mươi", "chín mươi"
    };

    public static String toWords(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        long value = amount.longValue();
        if (value == 0) {
            return "không đồng";
        }
        String result = readBlock(value);
        return (result + " đồng chẵn").replaceAll("\\s+", " ").trim();
    }

    private static String readBlock(long n) {
        String[] units = {"", " nghìn", " triệu", " tỷ", " nghìn tỷ", " triệu tỷ"};
        StringBuilder sb = new StringBuilder();
        int unitIndex = 0;
        while (n > 0) {
            int block = (int) (n % 1000);
            if (block != 0) {
                String blockWords = readThreeDigits(block);
                sb.insert(0, blockWords + units[unitIndex] + " ");
            }
            n /= 1000;
            unitIndex++;
        }
        return sb.toString().trim();
    }

    private static String readThreeDigits(int n) {
        StringBuilder sb = new StringBuilder();
        int hundreds = n / 100;
        int remainder = n % 100;
        if (hundreds > 0) {
            sb.append(ONES[hundreds]).append(" trăm ");
        } else if (remainder > 0 && hundreds == 0) {
            // không thêm gì
        }
        if (remainder > 0) {
            if (hundreds > 0 && remainder < 10) {
                sb.append("lẻ ");
            }
            sb.append(readTwoDigits(remainder));
        }
        return sb.toString().trim();
    }

    private static String readTwoDigits(int n) {
        if (n < 10) {
            return ONES[n];
        } else if (n < 20) {
            return TEENS[n - 10];
        } else {
            int tens = n / 10;
            int ones = n % 10;
            StringBuilder sb = new StringBuilder(TENS[tens]);
            if (ones > 0) {
                sb.append(" ");
                if (ones == 1) {
                    sb.append("mốt");
                } else if (ones == 5) {
                    sb.append("lăm");
                } else {
                    sb.append(ONES[ones]);
                }
            }
            return sb.toString();
        }
    }
}
