package net.byteseek.swing.treetable.demo;

import java.util.ArrayList;
import java.util.List;

public class TestClass {

    private String description;
    private long   size;
    private boolean enabled;
    private List<TestClass> children;
    private TestClass parent;

    public TestClass(String description, long size, boolean enabled) {
        this.description = description;
        this.size = size;
        this.enabled = enabled;
        children = new ArrayList<>();
    }

    public String getDescription() {
        return description;
    }

    public long getSize() {
        return size;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void addChild(TestClass test) {
        children.add(test);
    }

    public void addChildren(List<TestClass> children) {
        this.children.addAll(children);
    }

    public List<TestClass> getChildren() {
        return children;
    }
}
