package de.kfzteile24.salesOrderHub.helper;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@UtilityClass
public class DateFormatUtil {

    public final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String format(LocalDateTime localDateTime, DateTimeFormatter dateTimeFormatter) {
        return Optional.ofNullable(localDateTime)
                .map(dt -> dt.format(dateTimeFormatter))
                .orElse(StringUtils.EMPTY);
    }

    public String format(LocalDateTime localDateTime) {
        return Optional.ofNullable(localDateTime)
                .map(dt -> dt.format(FORMATTER))
                .orElse(StringUtils.EMPTY);
    }

    public String format(LocalDateTime localDateTime, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return DateFormatUtil.format(localDateTime, formatter);
    }
}
