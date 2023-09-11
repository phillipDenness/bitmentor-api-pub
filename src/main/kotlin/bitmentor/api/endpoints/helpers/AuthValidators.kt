package bitmentor.api.endpoints.helpers

import bitmentor.api.exceptions.InvalidRegexException
import java.util.regex.Pattern

fun checkEmail(email: String) {
    if (!EMAIL_ADDRESS_PATTERN.matcher(email).matches()) {
        throw InvalidRegexException("Email must be valid")
    }
}

fun isValidPassword(str: String): Boolean {
    var valid = true

    // Password policy check
    // Password should be minimum minimum 8 characters long
    if (str.length < 8) {
        valid = false
    }
    // Password should contain at least one number
    var exp = ".*[0-9].*"
    var pattern = Pattern.compile(exp, Pattern.CASE_INSENSITIVE)
    var matcher = pattern.matcher(str)
    if (!matcher.matches()) {
        valid = false
    }

    // Password should contain at least one capital letter
    exp = ".*[A-Z].*"
    pattern = Pattern.compile(exp)
    matcher = pattern.matcher(str)
    if (!matcher.matches()) {
        valid = false
    }

    // Password should contain at least one small letter
    exp = ".*[a-z].*"
    pattern = Pattern.compile(exp)
    matcher = pattern.matcher(str)
    if (!matcher.matches()) {
        valid = false
    }

    // Password should contain at least one special character
    // Allowed special characters : "~!@#$%^&*()-_=+|/,."';:{}[]<>?"
//    exp = ".*[~!@#\$%\\^&*()\\-_=+\\|\\[{\\]};:'\",<.>/?].*"
//    pattern = Pattern.compile(exp)
//    matcher = pattern.matcher(str)
//    if (!matcher.matches()) {
//        valid = false
//    }

    if (!valid) {
        throw InvalidRegexException(PASSWORD_POLICY)
    }

    return valid
}


private val PASSWORD_POLICY = """Password should be minimum 8 characters long,
            |should contain at least one capital letter,
            |at least one small letter,
            |at least one number""".trimMargin()

private val EMAIL_ADDRESS_PATTERN: Pattern = Pattern.compile(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
)

