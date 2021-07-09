package parsing.lexer;

import io.FilePushbackReader;
import org.jetbrains.annotations.NotNull;
import parsing.Markable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static parsing.XMLToken.*;

public class Tokenizer implements Iterator<String>, Markable {
    private static final Pattern digitPattern = Pattern.compile(R_TOKEN_DIGIT);
    private static final Pattern letterPattern = Pattern.compile(R_TOKEN_LETTER);
    private static final Pattern charRangePattern = Pattern.compile(R_TOKEN_CHARRANGE);
    private static final Pattern attValSQPattern = Pattern.compile(R_TOKEN_ATTVAL_SQ);
    private static final Pattern attValDQPattern = Pattern.compile(R_TOKEN_ATTVAL_DQ);
    private static final Pattern charDataPattern = Pattern.compile(R_TOKEN_CHARDATA);
    public static boolean _DEBUG = false;
    private static String nextTok = ""; //these are safe to leak (yay for immutability!)
    private final FilePushbackReader fpr;
    private final StringBuilder sb = new StringBuilder();

    private final ArrayList<String> tokenBuffer = new ArrayList<>();
    private final ArrayList<Integer> marks = new ArrayList<>();

    private int line = 1;
    private int column = 0;
    private int tokIndex = 0;

    public Tokenizer(FilePushbackReader f) {
        fpr = f;
    }

    public static boolean isDigit(String s) {
        Matcher digitMatcher = digitPattern.matcher(s);
        return digitMatcher.matches();
    }

    public static boolean isLetter(String s) {
        Matcher letterMatcher = letterPattern.matcher(s);
        return letterMatcher.matches();
    }

    public static boolean isInCharRange(String s) {
        Matcher charRangeMatcher = charRangePattern.matcher(s);
        return charRangeMatcher.matches();
    }

    public static boolean isInAttValSQRange(String s) {
        Matcher attValSQMatcher = attValSQPattern.matcher(s);
        return attValSQMatcher.matches();
    }

    public static boolean isInAttValDQRange(String s) {
        Matcher attValDQMatcher = attValDQPattern.matcher(s);
        return attValDQMatcher.matches();
    }

    public static boolean isInCharDataRange(String s) {
        Matcher charDataMatcher = charDataPattern.matcher(s);
        return charDataMatcher.matches();
    }

    @Override
    public boolean hasNext() {
        return !nextTok.equals(TOKEN_EOF);
    }

    @Override
    public String next() {
        //get next character
        sb.setLength(0);

        nextTok = LEXER_ERROR; //return lexer error as default if nothing matches

        if (tokIndex < tokenBuffer.size()) {
            return tokenBuffer.get(tokIndex++);
        }

        advance(sb);

        if (sb.toString().equals(TOKEN_EOF)) {
            nextTok = TOKEN_EOF;
            try {
                fpr.close();
            } catch (IOException e) {
                System.err.println("IOException when closing file");
            }

            return nextTok;
        }

        switch (sb.toString()) {
            case TOKEN_MINUS:
            case TOKEN_SINGLE_QUOTE:
            case TOKEN_DOUBLE_QUOTE:
            case TOKEN_PARENTHESES_LEFT:
            case TOKEN_PARENTHESES_RIGHT:
            case TOKEN_PLUS:
            case TOKEN_COMMA:
            case TOKEN_COLON:
            case TOKEN_EQUALS:
            case TOKEN_SEMICOLON:
            case TOKEN_EXCLAMATION:
            case TOKEN_ASTERISK:
            case TOKEN_HASHTAG:
            case TOKEN_AT_SIGN:
            case TOKEN_DOLLAR:
            case TOKEN_UNDERSCORE:
            case TOKEN_PERCENT:
            case TOKEN_CARET:
            case TOKEN_BIGGER_THAN:
            case TOKEN_WHITESPACE:
            case TOKEN_TAB:
            case TOKEN_CARRIAGE_RETURN:
            case TOKEN_DOT:
                nextTok = sb.toString();
                break;
            case TOKEN_SLASH:
                nextTok = TOKEN_SLASH;
                {
                    String peekChar = peek();
                    // '/>'
                    if (">".equals(peekChar)) {
                        nextTok = TOKEN_EMPTY_TAG_END;
                        sb.append("/>");
                        eat();
                    }
                }
            break;
            case TOKEN_QUESTION_MARK:
                nextTok = TOKEN_QUESTION_MARK;
                {
                    //?>
                    String peekChar = peek();
                    if (">".equals(peekChar)) {
                        nextTok = TOKEN_XML_DECL_END;
                        sb.append(">");
                        eat();
                    }
                }
            break;
            case TOKEN_LINEFEED:
                nextTok = TOKEN_LINEFEED;
                ++line;
                column = 0;
                break;
            case TOKEN_SMALLER_THAN:
                nextTok = TOKEN_SMALLER_THAN;

                {
                    String peekChar = peek();
                    String peekString;
                    switch (peekChar) {
                        // </
                        case "/":
                            nextTok = TOKEN_END_TAG_START;
                            sb.append("/");
                            eat();
                            break;
                        // <?
                        case "?":
                            peekString = multiPeek(4);
                            // ?xml
                            if (peekString.equals("?xml")) {
                                nextTok = TOKEN_XML_DECL_START;
                                sb.append("?xml");
                                eat(4);
                            }
                            break;
                        // <!
                        case "!":
                            peekString = multiPeek(3);
                            // <!--
                            if (peekString.equals("!--")) {
                                eat(3);
                                sb.setLength(sb.length() - 1); // pop stringbuilder
                                //TODO eatComment();
                                //TODO After calling eatcomment, and we've skipped the comment,
                                // we should probably call next() again, so we get a valid token to return.
                                // this way, we'll be avoiding using <-- as a SKIP token...
                                // interestingly enough, we can skip nested comments, if we call
                                // this method again if we spot a <!--
                                // however, nested comments are prohibited by the standard if IIRC
                            }
                            break;
                    }
                }
                break;
            default:
                String peek;
                peek = multiPeek(6);
                if (sb.toString().equals("v") && peek.equals("ersion")) {
                    eat(6);
                    nextTok = TOKEN_VERSION_KEYWORD;
                    break;
                }
                peek = multiPeek(2);
                if (sb.toString().equals("1") && peek.equals(".0")) {
                    eat(2);
                    nextTok = TOKEN_VERSION_1_0;
                    break;
                }

                peek = multiPeek(9);
                if (sb.toString().equals("s") && peek.equals("tandalone")) {
                    eat(9);
                    nextTok = TOKEN_STANDALONE_KEYWORD;
                    break;
                }

                peek = multiPeek(2);
                if (sb.toString().equals("y") && peek.equals("es")) {
                    eat(2);
                    nextTok = TOKEN_YES_KEYWORD;
                    break;
                }

                peek = peek();
                if (sb.toString().equals("n") && peek.equals("o")) {
                    eat();
                    nextTok = TOKEN_NO_KEYWORD;
                    break;
                }

                // letter
                if (isLetter(sb.toString())) {
                    nextTok = sb.toString();
                    break;
                }

                //digit
                if (isDigit(sb.toString())) {
                    nextTok = sb.toString();
                    break;
                }

                break;
        }

        if (nextTok.equals(LEXER_ERROR))
            error(line, column, String.format("Lexer found unknown token %s stacktrace\n%s",
                    sb.toString(), getCurrentStackTrace()));

        tokenBuffer.add(nextTok);
        ++tokIndex;

        return nextTok;
    }

    private String multiPeek(int n) {
        try {
            return fpr.peekChars(n);
        } catch (IOException e) {
            error(line, column, e.getMessage());
            System.exit(-1);
        }
        return "";
    }

    private String peek() {
        try {
            return fpr.peekChar() + "";
        } catch (IOException e) {
            error(line, column, e.getMessage());
            System.exit(-1);
        }

        return "";
    }

    private void eat() {
        try {
            fpr.eat();
            ++column;
        } catch (IOException e) {
            error(line, column, e.getMessage());
            System.exit(-1);
        }
    }

    private void eat(int n) {
        try {
            fpr.eat(n);
            column += n;
        } catch (IOException e) {
            error(line, column, e.getMessage());
            System.exit(-1);
        }
    }

    private void advance(@NotNull StringBuilder builder) {
        try {
            builder.append(fpr.readChar());
            ++column;
        } catch (IOException e) {
            error(line, column, e.getMessage());
            builder.append(TOKEN_EOF);
        }
    }

    @Override
    public void remove() {
        //skip current tok
        next();
    }

    @Override
    public void mark() {
        //autoboxing typically has strong performance issues with autoboxing
        // explicitly using .valueOf() might reduce this in specific instances; however I'm not sure in this specific
        // example if it does. However, since it'd happen automatically, there's no real reason to avoid it, considering
        // we might do hundreds of marks in huge parse files
        // https://www.theserverside.com/blog/Coffee-Talk-Java-News-Stories-and-Opinions/Performance-cost-of-Java-autoboxing-and-unboxing-of-primitive-types
        marks.add(Integer.valueOf(tokIndex));
    }

    @Override
    public boolean hasMark() {
        return !marks.isEmpty();
    }

    @Override
    public void consumeMark() {
        tokIndex = marks.remove(marks.size() - 1);
    }

    @Override
    public void unmark() {
        marks.remove(marks.size() - 1);
    }

    @Override
    public void forEachRemaining(Consumer<? super String> action) {
        while (hasNext()) {
            String tok = next();
            action.accept(tok);
            //action.andThen() //maybe implement this, no real reason honestly, idk
        }
    }

    public void eatComment() {

    }

    private void error(int line, int column, String message) {
        if (_DEBUG)
            System.err.println("Tokenizer Line: " + line + " Column: " + column + "\n" + message);
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getFilename() {
        return fpr.getFilename();
    }

    //courtesy of stackoverflow!
    // https://stackoverflow.com/questions/1069066/how-can-i-get-the-current-stack-trace-in-java
    private String getCurrentStackTrace() {
        return Arrays.toString(Thread.currentThread().getStackTrace());
    }

}
