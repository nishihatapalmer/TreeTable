package net.byteseek.swing.treetable;

public class TreeTableEvent {

    public interface Listener {
        boolean actionTreeEvent(TreeTableEvent event);
    }

    public enum TreeTableEventType {
        EXPANDING, COLLAPSING;
    }

    private final TreeTableNode node;
    private final TreeTableEventType eventType;

    public TreeTableEvent(TreeTableNode node, TreeTableEventType eventType) {
        this.node = node;
        this.eventType = eventType;
    }

    public TreeTableNode getNode() {
        return node;
    }

    public TreeTableEventType getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(Node: " + node + ", eventType: " + eventType.toString();
    }

}