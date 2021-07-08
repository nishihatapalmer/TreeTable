package net.byteseek.swing.treetable;

/**
 * An event in the tree table which may change the structure - a node is expanding or collapsing.
 */
public class TreeTableEvent {

    /**
     * A listener for TreeTableEvents.
     */
    public interface Listener {
        boolean actionTreeEvent(TreeTableEvent event);
    }

    /**
     * The type of events which can occur.
     */
    public enum TreeTableEventType {
        EXPANDING, COLLAPSING;
    }

    private final TreeTableNode node;
    private final TreeTableEventType eventType;

    /**
     * Constructs a TreeTable event given a TreeTableNode and an event type.
     * @param node The node to which the event is happening.
     * @param eventType The type of event.
     */
    public TreeTableEvent(TreeTableNode node, TreeTableEventType eventType) {
        this.node = node;
        this.eventType = eventType;
    }

    /**
     * @return the node the event relates to.
     */
    public TreeTableNode getNode() {
        return node;
    }

    /**
     * @return the type of event.
     */
    public TreeTableEventType getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(node: " + node + ", eventType: " + eventType.toString() + ')';
    }

}