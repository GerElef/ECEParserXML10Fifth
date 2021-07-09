package parsing.datastructs;

import java.util.ArrayList;
import java.util.HashMap;

public class Element {
    private final ArrayList<Element> children = new ArrayList<>();
    private final HashMap<String, String> attributeNameValues;
    private final ArrayList<String> contents = new ArrayList<>();
    private String tagName = null;

    public Element() {
        attributeNameValues = new HashMap<>(16);
    }

    public Element(String name, HashMap<String, String> attrNV) {
        tagName = name;
        attributeNameValues = attrNV;
    }

    public void insertChild(Element element) {
        children.add(element);
    }

    public void addContent(String content) {
        this.contents.add(content);
    }

    public void setAttributeValue(String attribute, String value) {
        attributeNameValues.replace(attribute, value);
    }

    public void addAttributeAndValue(String attribute, String value) {
        attributeNameValues.put(attribute, value);
    }

    public ArrayList<String> getContents() {
        return contents;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public HashMap<String, String> getAttributeNameValues() {
        //this should work, regardless of IDE warning
        return (HashMap<String, String>) attributeNameValues.clone();
    }

    //.clone() would be a shallow copy, doesn't make alot of sense for reducing leaks, so just leak the obj
    // in this implementation. if this were a library we should obviously protect the class instance
    public ArrayList<Element> getChildren() {
        return children;
    }

    public void printSelfAndChildren(String initialFormat) {
        //TODO convert this to a prettier format with / \ etc. and not tabular.
        System.out.println(initialFormat + tagName + " Attr: " + attributeNameValues.toString() + " {");
        for (String content : contents) {
            System.out.println(initialFormat + "Content: " + content);
        }
        for (Element child : children) {
            child.printSelfAndChildren(initialFormat + initialFormat);
        }
        System.out.println(initialFormat + "};");
    }
}
