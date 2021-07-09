package parsing.datastructs;

public class XMLDocument {
    private final String encoding = "UTF-8";
    private String version = "";
    private boolean standalone = true;
    private Element root = new Element();

    public XMLDocument() {
    }

    public String getEncoding() {
        return encoding;
    }

    public Boolean getStandalone() {
        return standalone;
    }

    public void setStandalone(Boolean standalone) {
        this.standalone = standalone;
    }

    public Element getRoot() {
        return root;
    }

    public void setRoot(Element root) {
        this.root = root;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
