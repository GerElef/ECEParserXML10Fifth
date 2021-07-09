import io.FilePushbackReader;
import parsing.exceptions.IllFormedXMLException;
import parsing.lexer.Tokenizer;
import parsing.synal.XMLAutomata;

import java.io.FileNotFoundException;

/**
 * XML parser and validator, by extent. It implements a small set of the entirety of the XML spec.
 * <a href="https://www.w3.org/TR/2008/REC-xml-20081126/">XML Spec</a>
 * <a href="https://cs.lmu.edu/~ray/notes/xmlgrammar/">XML Grammar Summary the implementation was based on</a>
 * In this version of XML, everything is treated as if it's UTF-8. As a consequence, there is no declaration for
 * file encoding (it was optional regardless, and it was supposed to be found dynamically per Unicode standard
 * if it wasn't specified).
 * It is not interesting to implement this in this version, it's just a matter of looking at the start of the bytes and
 * figuring out from there if it's UTF-8 or UTF-16, which are the only encodings that fully XML Spec *compliant* parsers
 * *must* support. Since we're not at all bothered with being fully compliant, and are just interested in the academic
 * part of writing a parser, we have omitted a few elements of XML that were too bothersome.
 * <p>
 * Supported Syntax of XML
 * </p>
 * Document
 *
 * document  ::=  prolog element Misc*
 *
 * Character Range
 *
 * Char  ::= '\t' | '\n' | '\r' | [a-zA-Z0-9]
 *
 * Whitespace
 *
 * S  ::=  (' ' | '\t' | '\r' | '\n')+
 *
 * Names and Tokens
 *
 * NameChar  ::=  Letter | Digit |  '.' | '-' | '_' | ':'
 * Name      ::=  (Letter | '_' | ':') (NameChar)*
 *
 * Literals
 *
 * AttValue       ::=  '"' ([^<&"])* '"' |  "'" ([^<&'])* "'"
 *
 * Comments
 *
 * Comment  ::=  '<!--' ((Char - '-') | ('-' (Char - '-')))* '-->'
 *
 * Prolog
 *
 * prolog       ::=  XMLDecl? Misc*
 * XMLDecl      ::=  '<?xml' VersionInfo SDDecl? S? '?>'
 * VersionInfo  ::=  S 'version' Eq ("'" VersionNum "'" | '"' VersionNum '"')
 * Eq           ::=  S? '=' S?
 * VersionNum   ::=  '1.0'
 * Misc         ::=  Comment | S
 *
 * Standalone Document Declaration
 *
 * SDDecl  ::=  S 'standalone' Eq (("'" ('yes' | 'no') "'") | ('"' ('yes' | 'no') '"'))
 *
 * Elements, Tags and Element Content
 *
 * element       ::=  EmptyElemTag  | STag content ETag
 * STag          ::=  '<' Name (S Attribute)* S? '>'
 * Attribute     ::=  Name Eq AttValue
 * ETag          ::=  '</' Name S? '>'
 * content       ::=  CharData? (element CharData?)*
 * EmptyElemTag  ::=  '<' Name (S Attribute)* S? '/>'
 * CharData      ::=  [^<&]* - ([^<&]* ']]>' [^<&]*)
 *
 * Characters
 *
 * Letter         ::= [a-zA-Z]
 * Digit          ::= [0-9]
 *
 * <p>
 * XML Parser and validator, by extent. As of this version, the parser doesn't support binary data embedded
 * within two XML tags, and can only read UTF-8 XML.
 * </p>
 *
 * @author Gerasimos Eleftheriotis, UoP
 */

public class Main {

    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Usage: eceparser file1.xml file2.xml ...");
            System.exit(-1);
        }

        XMLAutomata._DEBUG = false;

        for (String arg : args) {
            System.out.println("Parsing " + arg);

            XMLAutomata parser;
            try {
                //start parsing with XMLAutomata, recursive descent parser /o/
                parser = new XMLAutomata(new Tokenizer(new FilePushbackReader(arg)));

                parser.parse();
                parser.printResultingTree();
                System.out.printf("Parsing %s success!%n", arg);
            } catch (FileNotFoundException e) {
                System.err.printf("%s file not found%n", arg);
            } catch (IllFormedXMLException e) {
                System.err.println("File " + arg + " Error: ");
                e.printStackTrace();
            }
        }

    }
}
