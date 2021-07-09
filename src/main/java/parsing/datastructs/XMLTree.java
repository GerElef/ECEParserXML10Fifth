package parsing.datastructs;

import parsing.exceptions.IllFormedXMLException;

public class XMLTree {

    Element root = null;

    public XMLTree() {
    }

    //this leaks the obj and it's contents, not a problem within the library
    public Element getRoot() {
        return root;
    }

    public void setRoot(Element root) throws IllFormedXMLException {
        //XML can't have more than 1 root
        if (this.root != null)
            throw new IllFormedXMLException();

        this.root = root;
    }
}
