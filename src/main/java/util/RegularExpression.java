package util;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegularExpression {

    public static boolean matchRegex(String stringToMatch, String commitKey) {
        Pattern pattern = Pattern.compile(commitKey + "+[^0-9]");
        Matcher matcher = pattern.matcher(stringToMatch);
        return matcher.find();
    }

    /*public static boolean checkString(String stringToMatch, String commitKey) {

        int i = stringToMatch.indexOf("BOOKKEEPER");

        try {
            if (i != -1) {
                if(i + commitKey.length() + 1 > stringToMatch.length()) {
                    return stringToMatch.substring(i).equals(commitKey);
                }
                return matchRegex(stringToMatch.substring(i, i + commitKey.length() + 1), commitKey);
            }
            if (Objects.equals(commitKey, "BOOKKEEPER-296")) {
                System.out.println("THE VALUE OF i: " + i);
            }
        } catch (StringIndexOutOfBoundsException e) {
            System.out.println(stringToMatch.substring(i));
            System.out.println(commitKey + " " + i);
            throw e;
        }

        return false;
    }*/
}
