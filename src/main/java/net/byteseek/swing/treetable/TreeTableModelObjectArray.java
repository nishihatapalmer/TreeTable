package net.byteseek.swing.treetable;

import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeNode;

/**
 * TreeTableModelObjectArray is a concrete implementation of the TreeTableModel,
 * designed for tree structures where each node contains an array of objects that represent the column values.
 * It provides methods to retrieve the values within the object arrays for
 * each column in the table representation of the tree.
 * <p>
 * This is intended to make it easy to work with arbitrary tabular data (e.g. parsed from a CSV file with a parent-id structure),
 * without having to hard code a subclass of TreeTableModel for each different tabular data structure you work with.
 * <p>
 * It assumes that the tree has been created using DefaultMutableTreeNodes, and that the user object
 * associated with them is an Object array containing the column values.  The index of the object in the array
 * must match the TableColumns defined, so column model index 0 is associated with the objects[0] value and so on.
 * You must also specify the TableColumnModel in the constructor, so it knows which columns to create in the TreeTable.
 * <p>
 * This implementation is read-only and does not allow modifying the values in the tables.
 */
public class TreeTableModelObjectArray extends TreeTableModel {

    /**
     * Represents the TableColumnModel used by the TreeTableModelObjectArray to define the structure
     * and configuration of the columns in the table. The column model determines column-related properties,
     * such as column count, column headers, and column width, and is essential for mapping Object array indices
     * from TreeTable nodes to corresponding table columns.
     *
     * This model is critical for the TreeTableModelObjectArray to correctly interpret the array of objects
     * associated with tree nodes as rows in the table, aligning each object to its respective column with the
     * guidance of the TableColumnModel.
     *
     * The columnModel is provided as a required argument during construction of the TreeTableModelObjectArray
     * and cannot be null. It enables the consistent translation of tree node data into a tabular form.
     */
    protected TableColumnModel columnModel;

    /**
     * Constructs a TreeTableModelObjectArray with the specified root node and column model.
     * This implementation assumes individual nodes contain arrays of objects representing
     * the column values and uses the provided TableColumnModel to define the table's structure.
     *
     * @param rootNode The root node of the tree structure. Must not be null.
     * @param columnModel The TableColumnModel that defines the table's column configuration. Must not be null.
     * @throws IllegalArgumentException if columnModel is null.
     */
    public TreeTableModelObjectArray(TreeNode rootNode, TableColumnModel columnModel) {
        super(rootNode);
        checkNull(columnModel, "columnModel");
        this.columnModel = columnModel;
    }

    @Override
    public Object getColumnValue(TreeNode node, int column) {
        final Object[] objects = TreeUtils.getUserObject(node);
        return objects == null? null : objects[column];
    }

    @Override
    protected TableColumnModel createTableColumnModel() {
        return columnModel;
    }

}
