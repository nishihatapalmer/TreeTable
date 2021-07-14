package net.byteseek.swing.treetable;

import java.awt.event.MouseEvent;

/**
 * An interface which decides if a click on a node is an expand or collapse click.
 */
public interface TreeClickHandler {

    /**
     * Decides if a click on a node is an expand or collapse click. returns true if it is.
     * <p>
     * Note that the column is the index of the column clicked in the TableColumnModel, and that mouse coordinates
     * are given in terms of the whole JTable.  So if your tree column has been moved to the right, for example, you
     * need to calculate the start position of that column.  There is a method on the TreeTableModel which can
     * calculate how much space columns to the left of a column take up (calculateWidthToLeft).
     *
     * @param node The node which was clicked on.
     * @param column The index of the column in the TableColumnModel which was clicked.
     *               Note that if the columns were dragged into new positions, the column model index will not match
     *               the index in the TableColumnModel.  If you need the column model index, get it from the TableColumn.
     * @param evt  The mouse event
     * @return true if there was a click on expand/collapse for this node.
     */
    boolean clickOnExpand(final TreeTableNode node, final int column, final MouseEvent evt);

}
