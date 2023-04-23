package model;

public class JavaClass {

    private final String name;
    private final String content;
    private final Version release;

    public JavaClass(String name, String content, Version release) {
        this.name = name;
        this.content = content;
        this.release = release;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }
}
