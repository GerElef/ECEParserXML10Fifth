package parsing.synal;

import parsing.Markable;

import java.util.Stack;

class TokenStack implements Markable {

    public static boolean _DEBUG = false;
    private final Stack<Stack<String>> markStates = new Stack<>();
    private Stack<String> tokens = new Stack<>();

    public void addTok(String tok) {
        tokens.add(tok);
    }

    public String popTok() {
        return tokens.pop();
    }

    public int getTokCount() {
        return tokens.size();
    }

    public boolean isEmptyTok() {
        return tokens.isEmpty();
    }

    @Override
    public void mark() {
        if (_DEBUG) {
            System.err.println("Marking token stack with index " + markStates.size() + " :");
            for (String token : tokens) {
                switch (token) {
                    case "\n":
                        System.err.print("\\n|");
                        break;
                    case "\r":
                        System.err.print("\\r|");
                        break;
                    case "\t":
                        System.err.print("\\t|");
                        break;

                    default:
                        System.err.print(token + "|");
                }
            }
            System.err.println();
        }
        markStates.add((Stack<String>) tokens.clone());
    }

    @Override
    public boolean hasMark() {
        return !markStates.isEmpty();
    }

    @Override
    public void consumeMark() {
        if (_DEBUG) {
            System.err.println("Current token stack is: ");
            for (String token : tokens) {
                switch (token) {
                    case "\n":
                        System.err.print("\\n|");
                        break;
                    case "\r":
                        System.err.print("\\r|");
                        break;
                    case "\t":
                        System.err.print("\\t|");
                        break;

                    default:
                        System.err.print(token + "|");
                }
            }
            System.err.println();
            System.err.println("consuming mark index " + (markStates.size() - 1));
        }

        tokens = markStates.pop();
        if (_DEBUG) {
            System.err.println("New token stack is: ");
            for (String token : tokens) {
                switch (token) {
                    case "\n":
                        System.err.print("\\n|");
                        break;
                    case "\r":
                        System.err.print("\\r|");
                        break;
                    case "\t":
                        System.err.print("\\t|");
                        break;

                    default:
                        System.err.print(token + "|");
                }
            }
            System.err.println();
        }
    }

    @Override
    public void unmark() {
        markStates.pop();
    }
}
