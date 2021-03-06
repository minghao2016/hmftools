package com.hartwig.hmftools.patientdb.dao;

import java.time.LocalDate;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.TableField;

final public class DatabaseUtil {

    private DatabaseUtil() {
    }

    public static final int DB_BATCH_INSERT_SIZE = 1000;

    @Nullable
    public static java.sql.Date sqlDate(@Nullable LocalDate date) {
        return date != null ? java.sql.Date.valueOf(date) : null;
    }

    @Nullable
    public static Double decimal(@Nullable Double number) {
        return number == null ? null : decimal(number.doubleValue());
    }

    public static double decimal(double number) {
        return round(number, 4);
    }

    private static double round(double number, int decimalPoints) {
        double multiplier = Math.pow(10, decimalPoints);
        return Math.round(number * multiplier) / multiplier;
    }

    @NotNull
    static String checkStringLength(@NotNull String str, @NotNull TableField<?, String> field) {
        int maxLength = field.getDataType().length();
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    public static double valueNotNull(@Nullable Double value) {
        return value != null ? value : 0D;
    }

    public static int valueNotNull(@Nullable Integer value) {
        return value != null ? value : 0;
    }

    public static byte valueNotNull(@Nullable Byte value) {
        return value != null ? value : 0;
    }

    @NotNull
    public static String valueNotNull(@Nullable String value) {
        return value != null ? value : Strings.EMPTY;
    }
}
