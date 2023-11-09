# ECEParserXML10Fifth
XML parser and validator, by extent. It implements a small set of the entirety of the XML spec. 
[XML Spec](https://www.w3.org/TR/2008/REC-xml-20081126/) - [XML Grammar Summary](https://cs.lmu.edu/~ray/notes/xmlgrammar/) the implementation was based on</a> In this version of XML, everything is treated as if it's UTF-8. 
As a consequence, there is no declaration for file encoding (it was optional regardless, and it was supposed to be found dynamically per Unicode standard if it wasn't specified). 
It is not interesting to implement this in this version, it's just a matter of looking at the start of the bytes and figuring out from there if it's UTF-8 or UTF-16, which are the only encodings that fully XML Spec *compliant* parsers *must* support. 
Since we're not at all bothered with being fully compliant, and are just interested in the academic part of writing a parser, we have omitted a few elements of XML that were too bothersome.
## Supported XML Syntax 
### Document
document  ::=  prolog element Misc*
### Character Range
Char  ::= '\t' | '\n' | '\r' | [a-zA-Z0-9]
### Whitespace
S  ::=  (' ' | '\t' | '\r' | '\n')+
### Names and Tokens
NameChar  ::=  Letter | Digit |  '.' | '-' | '_' | ':'
Name      ::=  (Letter | '_' | ':') (NameChar)*
### Literals
AttValue       ::=  '"' ([^<&"])* '"' |  "'" ([^<&'])* "'"
### Comments
Comment  ::=  '<!--' ((Char - '-') | ('-' (Char - '-')))* '-->'
### Prolog
prolog       ::=  XMLDecl? Misc*
XMLDecl      ::=  '<?xml' VersionInfo SDDecl? S? '?>'
VersionInfo  ::=  S 'version' Eq ("'" VersionNum "'" | '"' VersionNum '"')
Eq           ::=  S? '=' S?
VersionNum   ::=  '1.0'
Misc         ::=  Comment | S

### Standalone Document Declaration
SDDecl  ::=  S 'standalone' Eq (("'" ('yes' | 'no') "'") | ('"' ('yes' | 'no') '"'))

### Elements, Tags and Element Content
element       ::=  EmptyElemTag  | STag content ETag
STag          ::=  '<' Name (S Attribute)* S? '>'
Attribute     ::=  Name Eq AttValue
ETag          ::=  '</' Name S? '>'
content       ::=  CharData? (element CharData?)*
EmptyElemTag  ::=  '<' Name (S Attribute)* S? '/>'
CharData      ::=  [^<&]* - ([^<&]* ']]>' [^<&]*)

### Characters
Letter         ::= [a-zA-Z]
Digit          ::= [0-9]

## Project Status
Pet project; do not use! XML Parser and validator, by extent. As of this version, the parser doesn't support binary data embedded within two XML tags, and can only read UTF-8 XML.
