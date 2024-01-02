package namdh.dhbkhn.datn.service.utils;

import java.time.*;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.regex.Pattern;
import namdh.dhbkhn.datn.domain.enumeration.WeekDay;
import namdh.dhbkhn.datn.service.error.ResourceNotFoundException;
import org.apache.commons.lang3.StringUtils;

public class Utils {

    public static <T> T requireExists(Optional<T> optional, String message) {
        return optional.orElseThrow(() -> new ResourceNotFoundException(message));
    }

    /**
     * Truncate instant to hours to send email and calculate at UTC zone
     * @param date: date to truncate
     * @return date after truncate to hour
     */
    public static Instant truncateInstant(Instant date) {
        return date.truncatedTo(ChronoUnit.HOURS).atZone(ZoneOffset.UTC).toInstant();
    }

    /**
     * Check email is valid format or not
     * An email is valid is email has been format ****@**.**** . Email must have at least 2 characters after the dot
     * @param email
     * @return
     */
    public static boolean isValidEmail(String email) {
        Pattern pattern = Pattern.compile("^([a-zA-Z0-9_.-])+@(([a-zA-Z0-9-])+\\.)+([a-zA-Z0-9]{2,4})+$", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(email).matches();
    }

    /**
     * Replace all extra whitespace start, end and middle of text
     * @param text: text
     * @return handled text
     */
    public static String handleWhitespace(String text) {
        if (text != null) {
            return text.trim().replaceAll(" +", " ");
        }
        return null;
    }

    /**
     * Convert from 20231.0 to 20231
     * @param text
     * @return handled .0
     */
    public static String handleDoubleNumber(String text) {
        if (text != null) {
            return text.substring(0, text.indexOf("."));
        }
        return null;
    }

    public static boolean isAllSpaces(String text) {
        return StringUtils.isBlank(text) && StringUtils.isNotEmpty(text);
    }

    public static boolean nameRegex(String name) {
        String regex = "^[^$&+,:;=?@#|'<>.^*()%!-]*$";
        return name.matches(regex);
    }

    public static boolean dateRegex(String date) {
        String regex =
            "^(?:(?:31(\\/|-|\\.)(?:0?[13578]|1[02]))\\1|(?:(?:29|30)(\\/|-|\\.)(?:0?[13-9]|1[0-2])\\2))(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$|^(?:29(\\/|-|\\.)0?2\\3(?:(?:(?:1[6-9]|[2-9]\\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\\d|2[0-8])(\\/|-|\\.)(?:(?:0?[1-9])|(?:1[0-2]))\\4(?:(?:1[6-9]|[2-9]\\d)?\\d{4})$";
        return date.matches(regex);
    }
}
