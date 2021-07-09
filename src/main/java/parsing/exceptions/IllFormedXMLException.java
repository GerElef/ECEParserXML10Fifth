package parsing.exceptions;

public class IllFormedXMLException extends Exception {

    public IllFormedXMLException() {
    }

    public IllFormedXMLException(String unexpected_eof) {
        super(unexpected_eof);
    }
}
