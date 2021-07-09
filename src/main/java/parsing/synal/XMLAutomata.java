package parsing.synal;

import org.jetbrains.annotations.NotNull;
import parsing.Markable;
import parsing.datastructs.Element;
import parsing.datastructs.XMLDocument;
import parsing.exceptions.EOFXMLException;
import parsing.exceptions.IllFormedXMLException;
import parsing.lexer.Tokenizer;

import java.util.Arrays;
import java.util.HashMap;

import static parsing.XMLToken.*;

public class XMLAutomata implements Automata, Markable {
    public static boolean _DEBUG = false;

    private final Tokenizer toker;
    private final TokenStack tokStack = new TokenStack();
    private XMLDocument xmlDocument;
    private String currentToken = null;

    public XMLAutomata(Tokenizer toker) {
        this.toker = toker;
    }

    @Override
    public void parse() throws IllFormedXMLException {
        TokenStack._DEBUG = _DEBUG; // share the debug val
        Tokenizer._DEBUG = _DEBUG; // share the debug val

        //document  ::=  prolog element Misc*
        //start with prolog
        xmlDocument = new XMLDocument();
        try {
            mark();
            parseProlog();
            unmark();
        } catch (IllFormedXMLException e) {
            if (_DEBUG)
                error(toker.getLine(), toker.getColumn(), "XMLDecl does not exist! Message: \n" + e.getMessage());
            //according to the standard (but not the grammar), prolog is optional, so we will respect that
            consumeMark(); // and revert to the last known state
        } catch (EOFXMLException e) {
            if (_DEBUG)
                error(toker.getLine(), toker.getColumn(), "EOFXMLException while parsing XMLDecl! " +
                        "Message: \n" + e.getMessage());
            throw new IllFormedXMLException("Unexpected EOF");
        }

        //parse root element
        try {
            mark();
            xmlDocument.setRoot(parseElement());
            unmark();
        } catch (EOFXMLException e) {
            if (_DEBUG)
                error(toker.getLine(), toker.getColumn(), "EOFXMLException while parsing root element!" +
                        " Message: \n" + e.getMessage());
            throw new IllFormedXMLException("Unexpected EOF");
        }

        //parse misc, any results should probably be ignored in this impl (explained below)
        parseMisc(); //we don't care if we hit EOF here,
    }

    //Prolog should return an XML document with set encoding and standalone.
    private void parseProlog() throws IllFormedXMLException, EOFXMLException {
        parseXMLDecl();

        parseMisc();
    }

    //This is optional. XMLDecl can not exist, meaning we need to at least return an "empty" xml document if it doesn't.
    private void parseXMLDecl() throws IllFormedXMLException, EOFXMLException {
        parseWhitespace(); //whitespace up until XML decl is ignored

        advance();

        if (!tokenMeetsExpected(currentToken, TOKEN_XML_DECL_START)) {
            reportExpectedTokenError(currentToken, TOKEN_XML_DECL_START);
        }

        String version = parseVersionInfo();
        xmlDocument.setVersion(version);

        //we don't care if this exists
        try {
            mark();
            parseSDDecl();
            unmark();
        } catch (IllFormedXMLException e) {
            if (_DEBUG)
                error(toker.getLine(), toker.getColumn(), "SDDecl does not exist! Message:\n" + e.getMessage());
            //if it doesn't exist, it's okay, just continue from where we left off.
            consumeMark();
        }

        parseWhitespace();

        advance();

        //if it's TOKEN_XML_DECL_END, XML decl is valid, and done!
        if (!tokenMeetsExpected(currentToken, TOKEN_XML_DECL_END)) {
            reportExpectedTokenError(currentToken, TOKEN_XML_DECL_END);
        }

    }

    private String parseVersionInfo() throws IllFormedXMLException, EOFXMLException {
        String vers = "unspecified";
        if (!parseWhitespace())
            reportExpectedTokenError(currentToken, "whitespace rule match");

        advance();

        if (!tokenMeetsExpected(currentToken, TOKEN_VERSION_KEYWORD))
            reportExpectedTokenError(currentToken, TOKEN_VERSION_KEYWORD);

        parseEq();

        advance();

        //follow the single quote path, otherwise the double quote
        if (tokenMeetsExpected(currentToken, TOKEN_SINGLE_QUOTE)) {
            advance();

            if (!tokenMeetsExpected(currentToken, TOKEN_VERSION_1_0))
                reportExpectedTokenError(currentToken, TOKEN_VERSION_1_0);

            vers = TOKEN_VERSION_1_0;

            advance();
            if (!tokenMeetsExpected(currentToken, TOKEN_SINGLE_QUOTE))
                reportExpectedTokenError(currentToken, TOKEN_SINGLE_QUOTE);

            return vers;
        }

        if (tokenMeetsExpected(currentToken, TOKEN_DOUBLE_QUOTE)) {
            advance();
            if (!tokenMeetsExpected(currentToken, TOKEN_VERSION_1_0))
                reportExpectedTokenError(currentToken, TOKEN_VERSION_1_0);

            vers = TOKEN_VERSION_1_0;

            advance();
            if (!tokenMeetsExpected(currentToken, TOKEN_DOUBLE_QUOTE))
                reportExpectedTokenError(currentToken, TOKEN_DOUBLE_QUOTE);

            return vers;
        }

        return vers;
    }

    private void parseEq() throws IllFormedXMLException, EOFXMLException {
        parseWhitespace();
        advance();
        if (!tokenMeetsExpected(currentToken, TOKEN_EQUALS))
            reportExpectedTokenError(currentToken, TOKEN_EQUALS);

        parseWhitespace();
    }

    private void parseSDDecl() throws IllFormedXMLException, EOFXMLException {
        String standalone;

        parseWhitespace();

        advance();

        if (!tokenMeetsExpected(currentToken, TOKEN_STANDALONE_KEYWORD)) {
            reportExpectedTokenError(currentToken, TOKEN_STANDALONE_KEYWORD);
        }

        parseEq();

        advance();

        //follow the single quote path, otherwise the double quote
        if (tokenMeetsExpected(currentToken, TOKEN_SINGLE_QUOTE)) {
            advance();

            if (!tokenMeetsExpected(currentToken, TOKEN_YES_KEYWORD)
                    && !tokenMeetsExpected(currentToken, TOKEN_NO_KEYWORD)) {
                reportExpectedTokenError(currentToken, TOKEN_YES_KEYWORD + " or " + TOKEN_NO_KEYWORD);
            }

            standalone = currentToken;

            advance();
            if (!tokenMeetsExpected(currentToken, TOKEN_SINGLE_QUOTE)) {
                reportExpectedTokenError(currentToken, TOKEN_SINGLE_QUOTE);
            }

            xmlDocument.setStandalone(standalone.equals("yes"));

        }

        if (tokenMeetsExpected(currentToken, TOKEN_DOUBLE_QUOTE)) {
            advance();

            if (!tokenMeetsExpected(currentToken, TOKEN_YES_KEYWORD)
                    && !tokenMeetsExpected(currentToken, TOKEN_NO_KEYWORD)) {
                reportExpectedTokenError(currentToken, TOKEN_YES_KEYWORD + " or " + TOKEN_NO_KEYWORD);
            }

            standalone = currentToken;

            advance();
            if (!tokenMeetsExpected(currentToken, TOKEN_DOUBLE_QUOTE)) {
                reportExpectedTokenError(currentToken, TOKEN_DOUBLE_QUOTE);
            }

            xmlDocument.setStandalone(standalone.equals("yes"));

        }

    }

    //sometimes this is optional, some times it is not. as a result, this should return a boolean to identify it
    private boolean parseWhitespace() {
        //match one or more whitespace tokens (?) +
        return matchOneOrMoreTokens(token ->
                tokenMeetsExpected(currentToken, TOKEN_WHITESPACE)
                        || tokenMeetsExpected(currentToken, TOKEN_TAB)
                        || tokenMeetsExpected(currentToken, TOKEN_CARRIAGE_RETURN)
                        || tokenMeetsExpected(currentToken, TOKEN_LINEFEED)
        );
    }

    //Element should return the root (and since it's a recursive structure, all it's children within)
    // of the XML document.
    private Element parseElement() throws IllFormedXMLException, EOFXMLException {
        //two productions possible:
        // empty tag
        try {
            mark();
            Element el = parseEmptyElemTag();
            unmark();
            return el;
        } catch (IllFormedXMLException e) {
            //there's no need to report this yet, just revert to the latest known version
            consumeMark();
        }

        // STag content ETag
        parseWhitespace();
        Element elem = parseSTag();
        if (_DEBUG)
            System.err.println("Found STag " + elem.getTagName());

        parseWhitespace(); //these are added because (unlike classic over-the-wire XML) tons of whitespace exists
        parseContent(elem);

        if (_DEBUG)
            System.err.println("Parsed STag content succesfully! " + elem.getTagName());

        parseWhitespace();

        String name = parseETag();

        if (_DEBUG)
            System.err.println("Parsed ETag succesfully! " + elem.getTagName());

        parseWhitespace();

        if (!elem.getTagName().equals(name))
            throw new IllFormedXMLException();

        return elem;
    }

    private Element parseEmptyElemTag() throws IllFormedXMLException, EOFXMLException {
        parseWhitespace();

        advance();

        if (!tokenMeetsExpected(currentToken, TOKEN_SMALLER_THAN)) {
            reportExpectedTokenError(currentToken, TOKEN_SMALLER_THAN);
        }

        String name = parseName();

        //zero or more attributes
        mark();
        HashMap<String, String> attributes = parseZeroOrMoreAttributes();
        if (attributes.size() == 0) {
            consumeMark();
        } else {
            unmark();
        }

        parseWhitespace(); //optional. no need to check if this actually matched

        advance();

        if (!tokenMeetsExpected(currentToken, TOKEN_EMPTY_TAG_END)) {
            reportExpectedTokenError(currentToken, TOKEN_EMPTY_TAG_END);
        }

        parseWhitespace();

        return new Element(name, attributes);
    }

    private String parseName() throws IllFormedXMLException, EOFXMLException {
        mark();
        advance();
        if (!Tokenizer.isLetter(currentToken) &&
                !tokenMeetsExpected(currentToken, TOKEN_UNDERSCORE) &&
                !tokenMeetsExpected(currentToken, TOKEN_COLON)) {
            consumeMark();
            reportExpectedTokenError(currentToken,
                    TOKEN_SMALLER_THAN
                            + " or " + TOKEN_UNDERSCORE
                            + " or " + R_TOKEN_LETTER
            );
        }
        unmark();

        StringBuilder b = new StringBuilder();

        b.append(currentToken);

        while (true) {
            try {
                mark();
                b.append(parseNameChar());
            } catch (IllFormedXMLException e) {
                if (b.length() == 1)
                    throw e;
                consumeMark();
                break;
            }
            unmark();
        }

        return b.toString();
    }

    private String parseNameChar() throws IllFormedXMLException, EOFXMLException {
        advance();
        if (!Tokenizer.isLetter(currentToken) &&
                !Tokenizer.isDigit(currentToken) &&
                !tokenMeetsExpected(currentToken, TOKEN_DOT) &&
                !tokenMeetsExpected(currentToken, TOKEN_MINUS) &&
                !tokenMeetsExpected(currentToken, TOKEN_UNDERSCORE) &&
                !tokenMeetsExpected(currentToken, TOKEN_COLON)) {
            reportExpectedTokenError(currentToken,
                    TOKEN_DOT
                            + " or " + TOKEN_MINUS
                            + " or " + TOKEN_UNDERSCORE
                            + " or " + TOKEN_COLON
                            + " or " + R_TOKEN_LETTER
                            + " or " + R_TOKEN_DIGIT
            );
        }
        return currentToken;
    }

    private HashMap<String, String> parseZeroOrMoreAttributes() throws EOFXMLException {
        HashMap<String, String> hm = new HashMap<>(16);

        while (true) {
            try {
                mark();
                parseWhitespace(); // might or might not exist, do not care
                String[] keyVal = parseAttribute();
                hm.put(keyVal[0], keyVal[1]);
                unmark();
            } catch (IllFormedXMLException e) {
                //no problem here!
                consumeMark();
                return hm;
            }
        }
    }

    private String[] parseAttribute() throws IllFormedXMLException, EOFXMLException {
        String name = parseName();

        parseEq();

        String value = parseAttValue();

        return new String[]{name, value};
    }

    private String parseAttValue() throws IllFormedXMLException, EOFXMLException {
        advance();

        StringBuilder b = new StringBuilder();

        if (tokenMeetsExpected(currentToken, TOKEN_DOUBLE_QUOTE)) {
            while (true) {
                try {
                    mark();
                    advance();
                    if (!Tokenizer.isInAttValDQRange(currentToken))
                        reportExpectedTokenError(currentToken, R_TOKEN_ATTVAL_DQ);

                    unmark();

                    b.append(currentToken);
                } catch (IllFormedXMLException e) {
                    if (b.length() == 0)
                        throw e;
                    consumeMark();
                    break;
                }
            }

            advance();
            if (!tokenMeetsExpected(currentToken, TOKEN_DOUBLE_QUOTE))
                reportExpectedTokenError(currentToken, TOKEN_DOUBLE_QUOTE);

            return b.toString();
        }

        if (tokenMeetsExpected(currentToken, TOKEN_SINGLE_QUOTE)) {
            while (true) {
                try {
                    mark();
                    advance();
                    if (!Tokenizer.isInAttValSQRange(currentToken))
                        reportExpectedTokenError(currentToken, R_TOKEN_ATTVAL_SQ);

                    unmark();

                    b.append(currentToken);
                } catch (IllFormedXMLException e) {
                    if (b.length() == 0)
                        throw e;
                    consumeMark();
                    break;
                }
            }

            advance();
            if (!tokenMeetsExpected(currentToken, TOKEN_SINGLE_QUOTE))
                reportExpectedTokenError(currentToken, TOKEN_SINGLE_QUOTE);

            return b.toString();
        }

        reportExpectedTokenError(currentToken, TOKEN_DOUBLE_QUOTE + " or " + TOKEN_SINGLE_QUOTE);
        //unreachable code. why does intellij force my hand to return null?
        return null;
    }

    private Element parseSTag() throws IllFormedXMLException, EOFXMLException {
        advance();

        if (!tokenMeetsExpected(currentToken, TOKEN_SMALLER_THAN))
            reportExpectedTokenError(currentToken, TOKEN_SMALLER_THAN);

        String name = parseName();

        mark();
        HashMap<String, String> attributes = parseZeroOrMoreAttributes();
        if (attributes.size() == 0) {
            consumeMark();
        } else {
            unmark();
        }

        parseWhitespace();

        advance();

        if (!tokenMeetsExpected(currentToken, TOKEN_BIGGER_THAN))
            reportExpectedTokenError(currentToken, TOKEN_BIGGER_THAN);

        return new Element(name, attributes);
    }

    private void parseContent(Element ogElement) throws IllFormedXMLException {
        //CharData? (element CharData?)*
        try {
            //CharData?
            String content;
            mark();
            content = parseCharData();
            if (content.equals(""))
                consumeMark();
            else {
                unmark();
                ogElement.addContent(content);
            }

            //if we hit EOF while parsing XML elements, it's over, the doc is ill formed.
            // catch eof and raise illformed!
            //  (element CharData?)*
            while (true) {
                boolean hasNestedElement = false;
                //optional element rule
                try {
                    mark();
                    ogElement.insertChild(parseElement());
                    unmark();
                    hasNestedElement = true;

                } catch (IllFormedXMLException e) {
                    //it's entirely possible for an element not to have an element inside it (otherwise
                    // it'd be an infinite loop). if we do indeed error on this, there's no need to
                    // terminate. just reset from the last known well state, and continue from there
                    consumeMark();
                }

                //if there isn't a nested element (parsing child failed)
                // there cannot be CharData, and obviously, we've reached the end of our nesting.
                // then, break
                if (!hasNestedElement)
                    break;


                //CharData?
                mark();
                content = parseCharData();
                if (content.equals(""))
                    consumeMark();
                else {
                    unmark();
                    ogElement.addContent(content);
                }
            }
        } catch (EOFXMLException e) {
            error(toker.getLine(), toker.getColumn(), e.getMessage());
            throw new IllFormedXMLException("Unexpected EOF");
        }
    }

    private String parseCharData() throws EOFXMLException {

        StringBuilder b = new StringBuilder();

        while (true) {
            try {
                mark();
                advance();
                if (!Tokenizer.isInCharDataRange(currentToken))
                    reportExpectedTokenError(currentToken, R_TOKEN_CHARDATA);

                unmark();

                b.append(currentToken);
            } catch (IllFormedXMLException e) {
                consumeMark();
                break;
            }
        }

        return b.toString();

    }

    private String parseETag() throws IllFormedXMLException, EOFXMLException {
        advance();

        if (!tokenMeetsExpected(currentToken, TOKEN_END_TAG_START))
            reportExpectedTokenError(currentToken, TOKEN_END_TAG_START);

        String name = parseName(); //must match owner

        parseWhitespace();

        advance();

        if (!tokenMeetsExpected(currentToken, TOKEN_BIGGER_THAN))
            reportExpectedTokenError(currentToken, TOKEN_BIGGER_THAN);

        return name;
    }

    //this will probably be ignored in the end result. It's comments and whitespace anyways.
    private void parseMisc() {
        //comments are removed automatically by the tokenizer
        parseWhitespace();
    }

    private boolean matchOneOrMoreTokens(Matcher matcher) {
        mark();
        boolean raisedException = false;
        try {
            mark();
            advance();
            boolean matched = matcher.match(currentToken);

            //if it's not whitespace, it's probably a valid token for some else rule, so we need to revert it
            if (!matched) {
                consumeMark();
                return false;
            }

            int markCount = 1;
            while (matched) {
                mark();
                ++markCount;

                advance();
                matched = matcher.match(currentToken);
            }

            //revert the last tok, and "null" the rest marks.
            // last tok is probably the next token for the next rule
            consumeMark();
            for (int i = 1; i < markCount; i++) {
                unmark();
            }

            return true;
        } catch (EOFXMLException e) {
            raisedException = true;
            consumeMark();
        } finally {
            if (!raisedException)
                unmark();
        }

        return false;
    }

    private boolean tokenMeetsExpected(@NotNull String tok, @NotNull String expected) {
        return tok.equals(expected);
    }

    public void printResultingTree() {
        System.out.println("\"" + toker.getFilename() + "\"" + " XML Document Version " + xmlDocument.getVersion() +
                ",Standalone " + (xmlDocument.getStandalone() ? "yes" : "no") + ",Encoding " +
                xmlDocument.getEncoding() + ",Structure:");
        xmlDocument.getRoot().printSelfAndChildren("\t");
    }

    /**
     * this method, in use with mark/consume methods, provide a handy way
     * to go back and forth on already parsed tokens.
     **/
    private void advance() throws EOFXMLException {
        currentToken = toker.next();
        tokStack.addTok(currentToken);

        if (currentToken.equals(TOKEN_EOF))
            throw new EOFXMLException();
    }

    /**
     * There used to be a revert() method here; but it turned out to be hellish to keep track of what is reverted
     * and where, and how it should be. In retrospect, it could be done that way; but I really don't want to deal with
     * radically inconsistent state between tokenstack and tokenizer between different calls.
     **/

    @Override
    public void mark() {
        tokStack.mark();
        toker.mark();
    }

    @Override
    public boolean hasMark() {
        return tokStack.hasMark() || toker.hasMark();
    }

    /**
     * Consumes the mark, and returns the token arraylist to it's original state. Pos argument defines the
     * token index to start again from.
     **/
    @Override
    public void consumeMark() {
        tokStack.consumeMark();
        toker.consumeMark();
    }

    /**
     * Voids the mark
     **/
    public void unmark() {
        tokStack.unmark();
        toker.unmark();
    }

    private void reportExpectedTokenError(String tok, String expected) throws IllFormedXMLException {
        if (_DEBUG)
            error(toker.getLine(), toker.getColumn(), "Expected " + expected + ", got " +
                    tok + "\n " + Arrays.toString(Thread.currentThread().getStackTrace()).replace(',', '\n'));
        //revert();
        throw new IllFormedXMLException();
    }

    private void error(int line, int column, String message) {
        System.err.println("XMLAutomata Line: " + line + " Column: " + column + "\n" + message);
    }

}
