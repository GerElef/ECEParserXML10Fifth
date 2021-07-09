package structures;

import java.util.HashMap;

@SuppressWarnings("unchecked")
public class Element {
    private final HashMap<String, String> attributes = new HashMap<>(13);
    private String openingTag = null;
    private String closingTag = null;

    public Element(String openingTag, String closingTag) {
        this.openingTag = openingTag;
        this.closingTag = closingTag;
    }

    public Element() {

    }

    public String getOpeningTag() {
        return openingTag;
    }

    public void setOpeningTag(String openingTag) {
        this.openingTag = openingTag;
    }

    public String getClosingTag() {
        return closingTag;
    }

    public void setClosingTag(String closingTag) {
        this.closingTag = closingTag;
    }

    public void addKeyValuePair(String k, String v) {
        attributes.put(k, v);
    }

    //Shallow copy is enough
    public HashMap<String, String> getKeyValuePairs() {
        return (HashMap<String, String>) attributes.clone();
    }
}
