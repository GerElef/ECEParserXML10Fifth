package parsing;

public interface Markable {
    void mark();

    boolean hasMark();

    void consumeMark();

    void unmark();
}
