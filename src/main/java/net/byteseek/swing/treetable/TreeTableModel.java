/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2021, Matt Palmer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.byteseek.swing.treetable;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeNode;
import net.byteseek.utils.collections.BlockModifyArrayList;

//TODO: main bugs:
//                        TreeStructureChanged  rebuildnodes is weird on root (also try it on other nodes)
//TODO: bug - show root and filtering

//TODO: check logic
//  check logic around when child counts are updated, and whether there are any missing updates for tree building...

//TODO: expand-all by default option?  All nodes always visible?
//TODO: insert-expanded option?

/**
 * A tree table model which binds to a JTable as a TableModel given a root tree node.
 * It is abstract, as the implementor must provide a subclass implementing the following:
 * <ul>
 *     <li>what the column definitions are</li>
 *     <li>getter that maps columns to fields.</li>
 * </ul>
 *
 * <p><b>Usage</b></p>
 * To use it, instantiate a subclassed TreeTableModel with a root TreeNode, then bind it to a JTable.
 * <p>
 * There are other useful non-abstract methods subclasses can override to provide icons for the tree,
 * allow cells to be editable and set their values, and so on.
 */
public abstract class TreeTableModel extends AbstractTableModel implements TreeModelListener {

    /* *****************************************************************************************************************
     *                                         Constants
     */

    /*
     * Keys to register keystroke events against.
     */
    protected static final String EXPAND_NODE_KEYSTROKE_KEY = "treeTableExpandNode";
    protected static final String COLLAPSE_NODE_KEYSTROKE_KEY = "treeTableCollapseNode";
    protected static final String TOGGLE_EXPAND_NODE_KEYSTROKE_KEY = "treeTableToggleExpandNode";

    /**
     * The TableColumnModel model index of the column which renders the tree structure and responds to clicks on expand
     * or collapse handles.  This is fixed at 0, as making it mutable caused various issues with re-assigning renderers
     * and handlers. It was doable, but overly complex.  It is simpler to say that the first column in the TableColumnModel
     * is the column that handles the tree events. You can move columns in the TableColumnModel, so it does not display as the
     * first column, but it is always logically the first column.
     */
    protected static final int TREE_COLUMN_INDEX = 0;

    /**
     * Default visible width of a TableColumn created using the utility createColumn() methods, if you don't specify a width.
     */
    protected static final int DEFAULT_COLUMN_WIDTH = 75; //TODO: test to see if this is a good default.

    /**
     * Value returned by find methods if not found.
     */
    protected static final int NOT_LOCATED = -1;

    /* *****************************************************************************************************************
     *                                         Variables
     */

    /*
     * User modifiable properties of the model:
     */

    /**
     * The root node of the tree with which the model is initialised.  The root can be changed later  by calling setRoot().
     */
    protected TreeNode rootNode;

    /**
     * The JTable this TreeTableModel is current bound to.  Use bindTable() methods or unbindTable() methods to control.
     */
    protected JTable table; // the table currently bound to the tree model, or null if not currently bound.

    /**
     * Whether the root of the tree is visible in the tree or not.  Can be changed with setShowRoot().
     */
    protected boolean showRoot; // whether the root of the tree is shown.

    /*
     * Keystroke bindings for expanding, collapsing or toggling expansion of a node.
     */
    protected KeyStroke[] expandKeys = new KeyStroke[] { // key presses that expand a node.
                KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0, false),        // + on keypad
                KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0, false),       // + on primary row
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.SHIFT_DOWN_MASK, false)}; // + above = on UK/US keyboards.
    protected KeyStroke[] collapseKeys = new KeyStroke[] { // key presses that collapse a node.
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0, false),      // - on primary row
                KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0, false)};  // - on keypad
    protected KeyStroke[] toggleKeys = new KeyStroke[] { // key presses that toggle node expansion
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false)};

    /**
     * The list of sort keys defined for this model. It is never null - only empty if no keys defined.
     */
    protected List<? extends RowSorter.SortKey> sortKeys = Collections.emptyList();

    /**
     * Groups sibling nodes together based on a grouping comparator (e.g. has children or doesn't).
     */
    protected Comparator<TreeNode> groupingComparator;

    /**
     * A filter predicate applied to the tree.  Any nodes which the predicate returns true for are filtered,
     * null if not filtering.
     */
    private Predicate<TreeNode> filterPredicate;

    /**
     * The threshold number of visible nodes in the tree below which a linear scan will be used to find the model index
     * of a node in the tree rather than using a tree scan.
     * Defaults to 100 after profiling with the jmh benchmarking tool, which showed tree scans consistently outperformed
     * linear scans at around that size of visible nodes in the tree, and that unsurprisingly, linear scans get
     * consistently worse the larger the number of visible nodes, on average.
     * There are edge cases where this won't be true, such as a large and very flat or deep tree, but should not
     * be a lot worse than a linear scan of all the nodes in those cases.
     */
    protected final int linearScanThreshold = 100;

    /*
     * Cached/calculated properties of the model.
     */

    /**
     * The TableColumnModel used by this TreeTableModel, and which will be set on a bound JTable.
     * The method {@link #createTableColumnModel()} creates the model, which is an abstract method which must be provided
     * by the subclass of this model.  It defines the column structure of the table and any associated renderers or editors
     * for the data type of each column.
     */
    protected TableColumnModel columnModel;

    /**
     * A list of all the currently displayed nodes in the table.  This is essentially a view over the tree for the table.
     * It uses a BlockModifyArrayList, which is a type of ArrayList that supports block insert and removals as single
     * operations.  The normal ArrayList handles this by individual inserts and removals, each of which shifts the
     * remaining elements in the array around, giving O(n * m) performance rather than O(n + m) for the BlockModifyArrayList.
     */
    protected final BlockModifyArrayList<TreeNode> displayedNodes = new BlockModifyArrayList<>();

    /**
     * A map tracking which nodes are expanded, and how many visible children they have.
     * It's a WeakHashMap to allow nodes which are no longer referenced anywhere else to be garbage collected.
     * <p>
     * An entry in the map of a node indicates that the node should be expanded. Remove a node from the map to collapse it.
     * The value of the node key is the number of currently visible children in the table for that node (including sub-children).
     * This is automatically updated - when a change is made to the number of children of a node the updated totals are
     * propagated down the parents to the root, thus maintaining accurate child counts for each level of the tree.
     * <p>
     * Note that the expanded node count value is only valid for *currently visible nodes*.
     * Nodes can remain "expanded" but not actually visible.
     * This situation can happen when the expanded parent of an expanded child node is collapsed.
     * So the child and children of the collapsed parent are no longer visible.
     * However, the child itself is still in an expanded state and still has an entry in the map.
     * If the parent was ever re-expanded, the child and children of the child would also be made visible again.
     * Making visible again involves rebuilding the visible node structure and recalculating any counts as it builds.
     * <p>
     * This allows us to only worry about keeping visible node counts accurate and lets us ignore the invisible part
     * of the tree for most operations.  There isn't really a downside to this, since the count itself is of no
     * use when making nodes visible again.  All the children still need processing to determine which ones
     * should be shown (e.g. if filtered), which will give us the actual count in the process of making them visible.
     * The fact there is an entry, even if the count may be wrong, still tells us the only thing we need at that point,
     * which is that it should be expanded and its visible children now need to be rebuilt.
     */
    protected final Map<TreeNode, Integer> expandedNodeCounts = new WeakHashMap<>(); // If a TreeNode is removed and no longer used, it will be garbage collected.

    /*
     * Keyboard, mouse and tree events
     */
    protected MouseListener tableMouseListener;
    protected final List<ExpandCollapseListener> eventListeners = new ArrayList<>(2); // tree event listeners.
    protected TreeClickHandler clickHandler; // the handler which processes expand/collapse click events.

    /**
     * The old header renderer assigned to the JTable before we bound to it.
     * We hold on to it so we can replace it if we unbind the TreeTableModelm from the JTable.
     */
    protected TableCellRenderer oldHeaderRenderer;


    /* *****************************************************************************************************************
     *                                         Constructors
     */

    /**
     * Constructs a TreeTableModel given the root node, the number of columns required.
     * The root node will be displayed by default.
     *
     * @param rootNode The root node of the tree to display.
     * @throws IllegalArgumentException if the rootNode is null.
     */
    public TreeTableModel(final TreeNode rootNode) {
        this(rootNode, true);
    }

    /**
     * Constructs a TreeTableModel given the root node, the number of columns required and whether to show the root
     * of the tree.
     *
     * @param rootNode The root node of the tree to display.
     * @param showRoot Whether to show the root node of the tree.
     * @throws IllegalArgumentException if the rootNode is null.
     */
    public TreeTableModel(final TreeNode rootNode, final boolean showRoot) {
        checkNull(rootNode, "rootNode");
        this.rootNode = rootNode;
        this.showRoot = showRoot;
        refreshTree(true);
    }


    /* *****************************************************************************************************************
     *                             Abstract methods for subclasses to implement
     *
     * These methods define how a particular model obtains data and defines the columns of the table.
     * These are obviously user specific, and must be defined by the subclass.
     */

    /**
     * Returns the value of a node for a given column.
     * Subclasses should implement this to map a column to node values.
     *
     * @param node The node to get a column value for.
     * @param column The column to get the value for.
     * @return The value of the column for the node.
     */
    public abstract Object getColumnValue(TreeNode node, int column);

    /**
     * Creates a TableColumnModel to define what columns the JTable should display for your subclass.
     * This could be all available columns, or you can just add a few and add or remove them dynamically using
     * the model later.  The model used is cached and can be obtained by a call to {@link #getTableColumnModel()}.
     * <p>
     * If no special TreeCellRenderer is set for the tree column with model index zero, then a default tree
     * renderer will be added automatically after it is created.  The column with model index zero is always the
     * column that renders and handles the tree events.  You can move the display order of columns you create,
     * in the TableColumnModel, but the tree column is always the one with column model index zero.
     * <p>
     * You should generally include a TableColumn with model index zero, as that is the column that renders the
     * tree structure, and responds to clicks on expand or collapse handles.
     *
     * @return A TableColumnModel with the columns you wish to display in the tree table initially.
     */
    protected abstract TableColumnModel createTableColumnModel();


     /* *****************************************************************************************************************
     *                        Methods to bind and unbind this model to and from a JTable
     *
     * To use a TreeTableModel, it must be bound to a JTable.
     */

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     */
    public void bindTable(final JTable table) {
        bindTable(table, new TreeTableRowSorter(this), new TreeTableHeaderRenderer());
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     * <p><b>Default Sort Keys</b><p>
     * It allows you to also set the default sort keys for the row sorter.  Default sort keys are not simply
     * the initial set of sort keys to sort on.  They are the set of sort keys to be used when no other sort
     * has been set (i.e. an empty list of sort keys).  It allows you to always have a table sorted by something.
     * If any clicks on other headers end up with nothing sorted, the default sort keys are what the table will
     * be sorted as.  The default sort keys can be empty (but this is the default without specifying them).
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param defaultSortKeys The default sort keys the table will have if no other sort defined.
     */
    public void bindTable(final JTable table, final RowSorter.SortKey... defaultSortKeys) {
        bindTable(table, Arrays.asList(defaultSortKeys));
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     * <p><b>Default Sort Keys</b><p>
     * It allows you to also set the default sort keys for the row sorter.  Default sort keys are not simply
     * the initial set of sort keys to sort on.  They are the set of sort keys to be used when no other sort
     * has been set (i.e. an empty list of sort keys).  It allows you to always have a table sorted by something.
     * If any clicks on other headers end up with nothing sorted, the default sort keys are what the table will
     * be sorted as.  The default sort keys can be empty (but this is the default without specifying them).
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param defaultSortKeys The default sort keys the table will have if no other sort defined.
     */
    public void bindTable(final JTable table, final List<? extends RowSorter.SortKey> defaultSortKeys) {
        bindTable(table, new TreeTableRowSorter(this, defaultSortKeys), new TreeTableHeaderRenderer());
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param rowSorter The row sorter to use with the table.
     */
    public void bindTable(final JTable table, final RowSorter<TreeTableModel> rowSorter) {
        bindTable(table, rowSorter, new TreeTableHeaderRenderer());
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param headerRenderer The renderer to use to draw the table header.
     */
    public void bindTable(final JTable table,  final TableCellRenderer headerRenderer) {
        bindTable(table, new TreeTableRowSorter(this), headerRenderer);
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     * <p><b>Default Sort Keys</b><p>
     * It allows you to also set the default sort keys for the row sorter.  Default sort keys are not simply
     * the initial set of sort keys to sort on.  They are the set of sort keys to be used when no other sort
     * has been set (i.e. an empty list of sort keys).  It allows you to always have a table sorted by something.
     * If any clicks on other headers end up with nothing sorted, the default sort keys are what the table will
     * be sorted as.  The default sort keys can be empty (but this is the default without specifying them).
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param headerRenderer The renderer to use to draw the table header.
     * @param defaultSortKey The default sort key the table will have if no other sort defined.
     */
    public void bindTable(final JTable table,  final TableCellRenderer headerRenderer, final RowSorter.SortKey... defaultSortKey) {
        bindTable(table, new TreeTableRowSorter(this, Arrays.asList(defaultSortKey)), headerRenderer);
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.
     * <p><b>Default Sort Keys</b><p>
     * It allows you to also set the default sort keys for the row sorter.  Default sort keys are not simply
     * the initial set of sort keys to sort on.  They are the set of sort keys to be used when no other sort
     * has been set (i.e. an empty list of sort keys).  It allows you to always have a table sorted by something.
     * If any clicks on other headers end up with nothing sorted, the default sort keys are what the table will
     * be sorted as.  The default sort keys can be empty (but this is the default without specifying them).
     *
     * @param table The JTable to bind to this TreeTableModel.
     * @param headerRenderer The renderer to use to draw the table header.
     * @param defaultSortKeys The default sort keys the table will have if no other sort defined.
     */
    public void bindTable(final JTable table,  final TableCellRenderer headerRenderer, final List<? extends RowSorter.SortKey> defaultSortKeys) {
        bindTable(table, new TreeTableRowSorter(this, defaultSortKeys), headerRenderer);
    }

    /**
     * Binds a JTable to use this model and configures columns, sorters and listeners to
     * react to mouse and keyboard events.  Also unbinds from any table which is currently bound.
     *
     * @param tableToBind The JTable to bind to this TreeTableModel.  Cannot be null.
     * @param rowSorter The row sorter to use with the table.  Can be null if no sorting required.
     * @param headerRenderer The renderer to use to draw the table header.  Can be null if no special header rendering required.
     */
    public void bindTable(final JTable tableToBind,
                          final RowSorter<TreeTableModel> rowSorter,
                          final TableCellRenderer headerRenderer) {
        if (this.table != null) {
            unbindTable();
        }
        this.table = tableToBind; // fine to set to null if no table is being bound - we'll just remain unbound.
        if (tableToBind != null) {
            tableToBind.setAutoCreateColumnsFromModel(false);
            tableToBind.setModel(this);
            tableToBind.setColumnModel(getTableColumnModel());
            if (rowSorter != null) {
                rowSorter.setSortKeys(sortKeys);
                tableToBind.setRowSorter(rowSorter);
            }
            oldHeaderRenderer = tableToBind.getTableHeader().getDefaultRenderer();
            if (headerRenderer != null) {
                tableToBind.getTableHeader().setDefaultRenderer(headerRenderer);
            }
            addKeyboardActions();
            addMouseListener();
        }
    }

    /**
     * Unbinds the model from the JTable.  Does nothing if the Jtable isn't using this as its TableModel.     *
     */
    public void unbindTable() {
        if (table != null && table.getModel() == this) {
            removeMouseListener();
            removeKeyboardActions();
            removeRowSorterAndCacheSortKeys();
            table.setColumnModel(new DefaultTableColumnModel());
            table.setAutoCreateColumnsFromModel(true); //TODO: should we cache old table setting?
            table.setModel(new DefaultTableModel());
            setTableHeaderRenderer(oldHeaderRenderer);
            table = null;
            oldHeaderRenderer = null;
        }
    }


    /* *****************************************************************************************************************
     *                             Optional methods for subclasses to implement
     *
     * These methods currently have blank implementations.  Override the ones you need.
     */

    /**
     * Returns an icon for a given node, to be rendered against the node.
     * The base implementation always returns null.
     * Override this method to provide an icon for a tree node.
     *
     * @param node The node to get an icon for.
     * @return The icon for that node, or null if no icon is defined.
     */
    public Icon getNodeIcon(final TreeNode node) {
        return null; // Default is no icons - override this method to return the icon for a tree node.
    }

    /**
     * Sets the value of the column to the node.
     * The base implementation does nothing.
     * Override this method if you want to be able to set the value of tree nodes by column.
     * You must implement this method if you want cells to be editable in the JTable in-place.
     *
     * @param node The node to set a column value for.
     * @param column The column to set the value for.
     * @param value The value to set.
     */
    public void setColumnValue(final TreeNode node, final int column, final Object value) {
        // Default is read-only.  Subclasses must override to set column values.
    }

    /**
     *  Return true if the cell at rowIndex, columnIndex is editable.
     *  The base implementation always returns false.
     *  Override this method if you want to make table cells editable.
     *
     *  @param  rowIndex  the row of the cell to edit.
     *  @param  columnIndex the column of the cell to edit.
     *  @return true if the cell at rowIndex / columnIndex is editable. //TODO: what are row index = model or table row?
     */
    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return false; // Default is read-only.  Subclasses must override to set whether a cell is editable.
    }

    /**
     * Returns the class of Object used in the data for each column, given the column index.
     * This allows the JTable to pick cell renderers for the types:
     * Object, String, Double, Float, Number, Icon, IconImage and Date,
     * and default cell editors for Object, String, Boolean, and Number, if you have not specified your own in the TableColumns.
     * <p>
     * If you are specifying your own cell renderers or cell editors in the TableColumns in the TableColumnModel,
     * then you do not need to override this method.
     * If you want to use the default renderers or editors in JTable,
     * you should override this method to provide the appropriate types from the list of basic types above.
     * If none is specified or otherwise available, JTable will fall back on rendering the string value of the Object.
     *
     * @param column The column to return the object class for.
     * @return The class of object for values in the column.
     */
    @Override
    public Class<?> getColumnClass(final int column) {
        return Object.class;
    }

    /**
     * Returns a Comparator for a given column index.
     * The base implementation always returns null - override this method if you want to specify custom comparators.
     * If null, then the model will compare node values directly if they implement Comparable,
     * or compare on the string value of the objects if they are not.
     *
     * @param column The column to return a Comparator for, or null if the default comparison is OK.
     * @return A Comparator for the given column, or null if no special comparator is required.
     */
    public Comparator<?> getColumnComparator(final int column) {
        return null; // Defaults to no special column comparators.
    }


    /* *****************************************************************************************************************
     *                                         Node grouping and sorting
     *
     * Gets and sets node grouping and sorting.
     */

    /**
     * Returns true if a grouping comparator is set on the model.
     * A grouping comparator can be set regardless of whether the model is bound to an actual table or not.
     * @return true true if a grouping comparator is set on the model.
     */
    public boolean isGrouping() {
        return groupingComparator != null;
    }

    /**
     * Returns the grouping comparator set on the model, or null if not set.
     * The grouping comparator is used to group sibling nodes together by some criteria (e.g. all folders).
     *
     * @return a grouping Comparator for a node, or null if not set.
     */
    public Comparator<TreeNode> getGroupingComparator() {
        return groupingComparator;
    }

    /**
     * Sets the node comparator to use, or null if no node comparisons are required.
     * When a node comparator is set, nodes are always sorted first by that comparator, before any column sorts
     * are applied.  This allows nodes to be grouped by some feature of the nodes.
     *
     * @param nodeComparator the node comparator to use, or null if no node comparisons are required.
     */
    public void setGroupingComparator(final Comparator<TreeNode> nodeComparator) {
        if (this.groupingComparator != nodeComparator) {
            this.groupingComparator = nodeComparator;
            fireTableDataChanged();
        }
    }

    /**
     * Returns true if sort keys are set.
     * @return true if any sort keys are set.
     */
    public boolean isSorting() {
        return !getSortKeys().isEmpty();
    }

    /**
     * Returns an unmodifiable list of sort keys currently set, or an empty list if none are set.
     * If no table is bound, it will just be the ones cached in this model for binding later.
     * If a table is bound, then the sort keys currently set on its row sorter will be returned.
     *
     * @return an unmodifiable list of sort keys currently set, or an empty list of no keys are set.
     */
    public List<? extends RowSorter.SortKey> getSortKeys() {
        if (table == null) {
            return sortKeys;
        }
        final RowSorter<? extends TableModel> rowSorter = table.getRowSorter();
        return rowSorter == null ? Collections.emptyList() : rowSorter.getSortKeys();
    }

    /**
     * Sets the sort keys to sort the model with.  If you pass in null, an empty list will be set.
     * You can set sort keys before binding occurs, and they will be cached for future binding.
     * If bound to a table, it will also set those sort keys on its RowSorter,
     * and create a TreeTableRowSorter for the table if one does not already exist.
     *
     * @param keys The list of sort keys to sort with.
     */
    public void setSortKeys(final List<? extends RowSorter.SortKey> keys) {
        // Cache the keys set as an unmodifiable list of keys.
        sortKeys = keys == null || keys.isEmpty()? Collections.emptyList() : Collections.unmodifiableList(keys);

        // If we're bound to a table, set those keys on the table, creating a row sorter if one does not exist.
        if (table != null) { // We are bound to a table - use its RowSorter, or create one if it doesn't have one.
            RowSorter<? extends TableModel> rowSorter = table.getRowSorter();
            if (rowSorter == null) { // row sorter not set on table - create one to hold the sort keys.
                rowSorter = new TreeTableRowSorter(this);
                table.setRowSorter(rowSorter);
                rowSorter.setSortKeys(sortKeys);
            } else {
                rowSorter.setSortKeys(sortKeys); // we have a table and a rowsorter - just set the keys!
            }
        }
    }

    /**
     * Sets the sort keys to sort the model with.  If you pass in null, an empty list will be set.
     * You can set sort keys before binding occurs, and they will be cached for future binding.
     * If bound to a table, it will also set those sort keys on its RowSorter,
     * and create a TreeTableRowSorter for the table if one does not already exist.
     * <p>
     * This version of setSortKeys is useful to set keys programmatically,
     *  as you can just specify as many keys as you like as direct parameters.
     *
     * @param keys The list of sort keys to sort with.
     */
    public void setSortKeys(final RowSorter.SortKey... keys) {
        setSortKeys(keys == null ? Collections.emptyList() : Arrays.asList(keys));
    }

    /**
     * When unbinding from a table, we will no longer get sort keys from its rowsorter.
     * Take a copy of the last known state of sort keys in the table, if any are set.
     * This ensures that a call to getSortKeys() while bound remains consistent after unbinding.
     * Get and Set methods for sort keys remain consistent, whether bound to a table or not.
     */
    protected void removeRowSorterAndCacheSortKeys() {
        if (table != null) {
            RowSorter<? extends TableModel> rowSorter = table.getRowSorter();
            if (rowSorter != null) {
                sortKeys = rowSorter.getSortKeys();
            }
            table.setRowSorter(null);
        }
    }


    /* *****************************************************************************************************************
     *                                    Filtering methods
     *
     * Methods which allow filtering nodes from the tree.
     */


    //TODO: should the root be filtered if it is hidden?
    // This question is really about whether this method is saying a node IS being filtered out, or that it merely passes a filter condition.
    //  Right now, this is a very low level method that answers a simple question on filtering.
    //  Don't think it should have logic around the root node necessarily - check it's use and where root node filtering matters.
    //  Look at code around building tree and see how root node is handled when filtering is active.

    /**
     * @return true if a filter predicate is set.
     */
    public boolean isFiltering() {
        return filterPredicate != null;
    }

    /**
     * Returns true only if the node passed in is filtered out by an active filter, and false otherwise.
     * If the node passed in is null, or there is no filter set, then it will return false.
     * If there is a filter set, and the non-null node meets the filter conditions, then it returns true.
     * <p>
     * This method says nothing about whether a node would be visible in the tree.
     * Even if it isn't filtered, it still might not be visible for other reasons.
     * If you need to check visibility of a node in the tree (which takes account of filtering too),
     * use {@link #isVisible(TreeNode)}.
     *
     * @param node The node te test.
     * @return true if the node passed in matches an active filter.
     */
    public boolean isFiltered(final TreeNode node) {
        return node != null && filterPredicate != null && filterPredicate.test(node);
    }

    /**
     * Sets a filter predicate on the model.  Any nodes which meet the predicate will be filtered out.
     *
     * @param filterPredicate The predicate used to filter a node.  If the test returns true, the node is filtered.
     */
    public void setNodeFilter(final Predicate<TreeNode> filterPredicate) {
        if (this.filterPredicate != filterPredicate) {
            this.filterPredicate = filterPredicate;
            refreshTree(false);
            fireTableDataChanged();
        }
    }

    /**
     * @return The current filter assigned to the model, or null if no filter is set.
     */
    public Predicate<TreeNode> getNodeFilter() {
        return filterPredicate;
    }


    /* *****************************************************************************************************************
     *                                    TableModel interface methods.
     *
     * Methods required for a JTable to interact with this the data in this model.
     */

    @Override
    public int getRowCount() {
        return displayedNodes.size();
    }

    @Override
    public int getColumnCount() {
        return getTableColumnModel().getColumnCount();
    } //TODO: check it's OK to use column model for this (no infinite loops where model asks for count which asks model for count....).
    // DefaultTableModel manages its own columns and doesn't use a TableColumnModel at all :o

    //TODO: test column setting / getting works when columns are re-arranged visually.

    /*
     * {@inheritDoc}
     * <p>
     * Although it's not clear from the TableModel interface javadoc, "row" here refers to the model
     * index in the TableModel, not the actual row in the JTable, which may be different if sorted.
     * A JTable will automatically convert sorted rows back into the TableModel model index.
     */
    @Override
    public Object getValueAt(final int row, final int column) {
        return getColumnValue( getNodeAtModelIndex(row), column);
    }

    /*
     * {@inheritDoc}
     * <p>
     * Although it's not clear from the TableModel interface javadoc, "row" here refers to the model
     * index in the TableModel, not the actual row in the JTable, which may be different if sorted.
     * A JTable will automatically convert sorted rows back into the TableModel model index.
     */
    @Override
    public void setValueAt(final Object aValue, final int row, final int column) {
        setColumnValue( getNodeAtModelIndex(row), column, aValue);
    }

    /* ****************************************************************************************************************
     *                                Table methods
     *
     * Methods relating to the table we are bound to, if any.
     */

    /**
     * @return the JTable bound to this TreeTableModel, or null if one is not currently bound.
     */
    public JTable getTable() {
        return table;
    }

    /**
     * @return The renderer used for the table header, or null if not set.
     */
    public TableCellRenderer getTableHeaderRenderer() {
        if (table != null) {
            final JTableHeader header = table.getTableHeader();
            if (header != null) {
                return header.getDefaultRenderer();
            }
        }
        return null;
    }

    /**
     * Sets the renderer for the table header, if it exists.
     * @param renderer The renderer to use.
     */
    public void setTableHeaderRenderer(final TableCellRenderer renderer) {
        if (table != null) {
            final JTableHeader header = table.getTableHeader();
            if (header != null) {
                header.setDefaultRenderer(renderer);
            }
        }
    }


    /* *****************************************************************************************************************
     *                                    Tree Change methods
     *
     * Methods required for this model to listen for TreeModelListener events, or to manually notify the
     * model that a node has changed.  Note that the tree structure has *already* changed once these methods
     * are called, so a removed node will no longer be part of the actual tree even if it is currently being
     * displayed (so must be removed from display), or new nodes may have been added which are not currently displayed (so must be added).
     * This class can be subscribed to a DefaultTreeModel in order to respond to these tree events,
     * or you can call the change methods yourself if not using a tree model once the node structure has been changed.
     */

    /**
     * Refreshes the entire visible tree, rebuilding from the root upwards.
     * If you require a rebuild from scratch, then reset expanded nodes as well, as none of the existing expanded
     * nodes will be in the new tree. If you simply want to rebuild due to some node changes,
     * but would like to preserve expanded state of existing nodes if possible, then pass false to resetExpanded.
     *
     * @param resetExpanded Whether to reset expanded nodes - if true removes all node expansion (except root if not visible).
     */
    public void refreshTree(final boolean resetExpanded) {
        if (resetExpanded) {
            clearExpansions();   // wipe out any existing expansion, add the root as expanded if it's not visible.
        }
        buildVisibleNodes();   // rebuild all the visible nodes. //TODO: does this leave incorrect node counts when building if they already existed?
    }

    @Override
    public void treeNodesChanged(final TreeModelEvent e) {
        treeNodesChanged( getLastPathNode(e), e.getChildIndices());
    }

    /**
     * Notifies the model that a node has changed one of its properties, but its structure hasn't changed or its children.
     * @param nodeChanged The node that needs to be refreshed.
     */
    public void treeNodeChanged(final TreeNode nodeChanged) {
        if (isVisible(nodeChanged)) {
            final int modelIndex  = getModelIndexForTreeNode(nodeChanged);
            fireTableRowsUpdated(modelIndex, modelIndex);
        }
    }

    /**
     * Notifes the model that a set of siblings have changed (but not their children or child structure).
     *
     * @param parentNode The parent node of the children which changed.
     * @param childIndices The indices of the children who changed in the parent.
     */
    public void treeNodesChanged(final TreeNode parentNode, final int[] childIndices) {
        if (isExpanded(parentNode) && (parentNode == rootNode || isVisible(parentNode))) {
            if (childIndices == null) { // child indices of null indicate root node has changed according to treeNodesChanged(TreeModelEvent).
                if (showRoot) {
                    fireTableRowsUpdated(0, 0);
                }
            } else {
                for (int i = 0; i < childIndices.length; i++) {
                    final int modelIndex = getModelIndexForTreeNode(parentNode.getChildAt(childIndices[i]));
                    fireTableRowsUpdated(modelIndex, modelIndex);
                }
            }
        }
    }

    @Override
    public void treeNodesInserted(final TreeModelEvent e) {
        treeNodesInserted( getLastPathNode(e), e.getChildIndices());
    }

    /**
     * Informs the model that a new node was inserted into the parent node with the child index given.
     * @param parentNode The parent node that had a child inserted.
     * @param childIndex The index the child was inserted at.
     */
    public void treeNodeInserted(final TreeNode parentNode, final int childIndex) {
        if (parentNode == rootNode || (isExpanded(parentNode) && isVisible(parentNode))) {
            final int numInserted = insertChildNodeToModel(parentNode, childIndex);
            updateVisibleChildCounts(parentNode, numInserted);
        }
    }

    /**
     * Informs the model that new nodes were inserted into the parent node with the child indices given.
     *
     * @param parentNode The parent node that has new children.
     * @param childIndices The indices of the new children.
     */
    public void treeNodesInserted(final TreeNode parentNode, final int[] childIndices) {
        if (parentNode == rootNode || (isExpanded(parentNode) && isVisible(parentNode) && childIndices != null)) {
            final int numInserted = insertChildNodesToModel(parentNode, childIndices);
            updateVisibleChildCounts(parentNode, numInserted);
        }
    }

    @Override
    public void treeNodesRemoved(final TreeModelEvent e) {
        treeNodesRemoved(getLastPathNode(e), e.getChildIndices(), e.getChildren());
    }

    /**
     * Informs the model that child nodes have been removed from a parent node.
     *
     * @param previousParentNode The parent previously to the removed children.
     * @param childIndices The indices of the nodes removed.
     * @param removedChildren The child nodes which were removed.
     */
    public void treeNodesRemoved(final TreeNode previousParentNode, final int[] childIndices, final Object[] removedChildren) {
        if (childIndices != null && removedChildren != null &&
                (previousParentNode == rootNode || (isVisible(previousParentNode) && isExpanded(previousParentNode)))) {
            final int numRemoved = removeVisibleNodes(childIndices, removedChildren);
            updateVisibleChildCounts(previousParentNode, -numRemoved);
        }
    }

    /**
     * Informs the model that a child node was removed from a parent node.
     *
     * @param previousParentNode The parent previously to the removed child.
     * @param removedNode The removed child.
     */
    public void treeNodeRemoved(final TreeNode previousParentNode, final TreeNode removedNode) {
        if (previousParentNode == rootNode || (isVisible(previousParentNode) && isExpanded(previousParentNode))) {
            final int numRemoved = removeVisibleNode(removedNode);
            updateVisibleChildCounts(previousParentNode, -numRemoved);
        }
    }

    @Override
    public void treeStructureChanged(final TreeModelEvent e) {
        final TreeNode parentNode = getLastPathNode(e);
        if (e.getPath().length == 1 && parentNode != rootNode) { // only one object in path and not current root - set root.
            setRoot(parentNode);
        } else {
            treeStructureChanged(parentNode);
        }
    }

    /**
     * Informs the model that a node has changed its child structure a lot and so it should be rebuilt.
     *
     * @param changedNode The node whose child structure has changed.
     */
    public void treeStructureChanged(final TreeNode changedNode) {
        if (isVisible(changedNode) || isHiddenRoot(changedNode)) {
            if (isExpanded(changedNode)) {
                 rebuildVisibleChildren(changedNode);
            } else { // visible but no children visible - refresh the display of that node in the table.
                final int modelIndex = getModelIndexForTreeNode(changedNode);
                fireTableRowsUpdated(modelIndex, modelIndex);
            }
        }
    }

    /**
     * @param node The node to check. //TODO: only used once - check it shouldn't be used elsewhere or there's a good reason this test only matters once.
     * @return true if the node passed in is the root, and it's hidden.
     */
    protected boolean isHiddenRoot(final TreeNode node) {
        return !showRoot && node == rootNode;
    }

    /**
     * Rebuilds the tree from the changed node downwards.
     * Must only be called on a node which is visible in the tree.
     * @param changedNode The node whose children need rebuilding.
     */
    protected void rebuildVisibleChildren(final TreeNode changedNode) {
        if (changedNode == rootNode) { // handle a complete tree refresh differently using refreshTree (takes account of hidden status of root node).
            refreshTree(false); //TODO: would this change the node counts when re-built?
            fireTableDataChanged();
        } else {
            int firstChildModelIndex = NOT_LOCATED;

            // 1. remove any prior visible children - filtering not required for nodes which are already visible.
            final int numOldChildren = getExpandedChildCount(changedNode);
            if (numOldChildren > 0) {
                firstChildModelIndex = getModelIndexForTreeNode(changedNode) + 1;
                removeVisibleRows(firstChildModelIndex, firstChildModelIndex + numOldChildren - 1);
            }

            // 2. insert any current children:
            int numNewChildren = 0;
            if (changedNode.getChildCount() > 0) {
                if (firstChildModelIndex == NOT_LOCATED) { // don't recalculate model index if we already have it from removing.
                    firstChildModelIndex = getModelIndexForTreeNode(changedNode) + 1;
                }
                // Add children to display rebuilds the visible nodes, so filtering is taken care of.
                //TODO: this resets expanded counts for any nodes which remain visible, but sub-sub nodes that are still expanded
                //      may have incorrect visible counts.
                numNewChildren = addVisibleChildren(firstChildModelIndex, changedNode);
            }

            // 3. update visible node counts.
            expandedNodeCounts.put(changedNode, numNewChildren);
            updateTreeChildCounts(changedNode, numNewChildren - numOldChildren);
        }
    }

    /**
     * Sets the root of the tree to a new root node.
     *
     * @param newRoot The new root node for the tree.
     * @throws IllegalArgumentException if newRoot is null.
     */
    public void setRoot(final TreeNode newRoot) {
        checkNull(newRoot, "newRoot");
        if (newRoot != rootNode) {
            rootNode = newRoot;
            clearExpansions();
            buildVisibleNodes();
        }
    }

    protected void checkNull(Object object, String fieldName) {
        if (object == null) {
            throw new IllegalArgumentException("Object cannot be null: " + fieldName);
        }
    }

    protected TreeNode getLastPathNode(TreeModelEvent e) {
        return (TreeNode) e.getPath()[e.getPath().length - 1];
    }

    protected int insertChildNodesToModel(final TreeNode parentNode, final int[] childIndices) {
        final int length = childIndices.length;
        int from = 0;
        int numInserted = 0;
        while (from < length) {
            final int to = TreeUtils.findLastConsecutiveIndex(from, childIndices); // process all consecutive insertions together.
            if (from == to) { // just inserting a single node (that potentially also has children, so numinserted may be bigger than one).
                numInserted += insertChildNodeToModel(parentNode, childIndices[from]);
            } else {          // inserting a consecutive block of nodes.
                numInserted += insertChildNodesToModel(parentNode, childIndices, from, to);
            }
            from = to + 1;
        }
        return numInserted;
    }

    protected int insertChildNodeToModel(final TreeNode parentNode, final int childIndex) {
        final int insertPosition = getModelIndexAtInsertPosition(parentNode, childIndex, 1);
        if (insertPosition >= 0) { //TODO: why don't we throw an exception if the insertion indexes are out of bounds?
            final TreeNode insertedNode = parentNode.getChildAt(childIndex); // The node that was inserted.
            if (!isFiltered(insertedNode)) {
                final List<TreeNode> newNodes = new ArrayList<>( 64);
                newNodes.add(insertedNode);
                buildVisibleChildren(insertedNode, newNodes);
                displayedNodes.addAll(insertPosition, newNodes);
                fireTableRowsInserted(insertPosition, insertPosition + newNodes.size() - 1);
                return newNodes.size();
            }
        }
        return 0;
    }

    protected int insertChildNodesToModel(final TreeNode parentNode, final int[] childIndices, final int from, final int to) {
        final int insertPosition = getModelIndexAtInsertPosition(parentNode, childIndices[from], to - from + 1);
        if (insertPosition > 0) { //TODO: why don't we throw an exception if the insertion indexes are out of bounds?
            final List<TreeNode> newNodes = new ArrayList<>(128);
            for (int index = from; index <= to; index++) {
                final int childIndex = childIndices[index];
                final TreeNode insertedNode = parentNode.getChildAt(childIndex); // The node that was inserted.
                if (!isFiltered(insertedNode)) {
                    newNodes.add(insertedNode);
                    buildVisibleChildren(insertedNode, newNodes);
                }
            }
            if (newNodes.size() > 0) {
                displayedNodes.addAll(insertPosition, newNodes);
                fireTableRowsInserted(insertPosition, insertPosition + newNodes.size() - 1);
                return newNodes.size();
            }
        }
        return 0;
    }

    protected int removeVisibleNodes(final int[] childIndices, final Object[] removedChildren) {
        final int length = childIndices.length;
        int from = 0;
        int numRemoved = 0;
        while (from < length) {
            final int to = TreeUtils.findLastConsecutiveIndex(from, childIndices); // process all consecutive insertions together.
            if (from == to) { // just removing a single node (that potentially also has removedChildren, so numRemoved may be bigger than one).
                numRemoved += removeVisibleNode((TreeNode) removedChildren[from]);
            } else {          // removing a block of nodes.
                numRemoved += removeVisibleNodes(from, to, removedChildren);
            }
            from = to + 1;
        }
        return numRemoved;
    }

    protected int removeVisibleNode(final TreeNode removedNode) {
        final int modelIndex = getModelIndexForTreeNode(removedNode);
        if (modelIndex >= 0) {
            final int numChildren = getExpandedChildCount(removedNode);
            final int lastIndex = modelIndex + numChildren;
            displayedNodes.remove(modelIndex, lastIndex);
            fireTableRowsDeleted(modelIndex, lastIndex);
            return 1 + numChildren;
        }
        return 0; // wasn't visible after all - so nothing removed.
    }

    protected int removeVisibleNodes(final int from, final int to, final Object[] removedChildren) {
        final int modelIndex = getModelIndexForTreeNode((TreeNode) removedChildren[from]);
        if (modelIndex >= 0) {
            int numToRemove = 0;
            for (int index = from; index <= to; index++) {
                numToRemove += (getExpandedChildCount((TreeNode) removedChildren[index]) + 1);
            }
            final int lastIndex = modelIndex + numToRemove - 1;
            displayedNodes.remove(modelIndex, lastIndex);
            fireTableRowsDeleted(modelIndex, lastIndex);
            return numToRemove;
        }
        return 0; // wasn't visible after all - so nothing removed.
    }

    protected int getModelIndexAtInsertPosition(final TreeNode parentNode, final int firstInsertedIndex, final int numInsertions) {
        final int afterInsertionIndex = firstInsertedIndex + numInsertions;
        final int numChildren = parentNode.getChildCount();
        final int modelIndexToInsertAt;

        //TODO: error conditions check here?  Like if the number of children doesn't match the insertions being claimed...

        // No previous nodes (inserted into parent with no prior children):
        if (numInsertions == numChildren) {
            // Insert at the position just after the parent node.
            modelIndexToInsertAt = getModelIndexForTreeNode(parentNode) + 1;
        }

        // If inserted at the end of existing children (after the last insertion is the size of the children)
        else if (afterInsertionIndex == numChildren) {
            // previous last child is the one before the first inserted.
            final TreeNode previousChild = parentNode.getChildAt(firstInsertedIndex - 1);
            // insert one after last previous child and all its visible children. //TODO: null pointer exception.
            modelIndexToInsertAt = getModelIndexForTreeNode(previousChild) + getExpandedChildCount(previousChild) + 1;
        }

        // Inserting before the end of existing children:
        else {
            // insert at the index of the child the last inserted node displaced one on.
            modelIndexToInsertAt = getModelIndexForTreeNode(parentNode.getChildAt(afterInsertionIndex));
        }
        return modelIndexToInsertAt;
    }


    /* *****************************************************************************************************************
     *                                          Node and index getters
     *
     * Methods to get nodes, selected nodes, converting between model index and table rows.
     */

    /**
     * @return the root node of the tree.
     */
    public TreeNode getRoot() {
        return rootNode;
    }

    /**
     * Gets the node at the model row, or null if the model index is out of bounds.
     *
     * @param modelIndex The row in the display model to get the node for.
     * @return The node for the row in the (unsorted) model index, or null if out of bounds. //TODO: return null or throw indexoutofbounds?
     */
    public TreeNode getNodeAtModelIndex(final int modelIndex) {
        return modelIndex >= 0 && modelIndex < displayedNodes.size() ? displayedNodes.get(modelIndex) : null;
    }

    /**
     * Gets the node in a bound JTable given a table row index.
     * The view row index can differ from the rows in this underlying model if the table is sorted or filtered.
     *
     * @param tableRow The row in the table to get the node from.
     * @return The node at the tableRow position in the JTable.
     */
    public TreeNode getNodeAtTableRow(final int tableRow) {
        return getNodeAtModelIndex( getModelIndexForTableRow(tableRow));
    }

    /**
     * Gets the first node which is currently selected in the JTable, or null if no row is selected.
     *
     * @return the node which is currently selected in the JTable, or null if no row is selected.
     */
    public TreeNode getSelectedNode() {
        return table == null? null : getNodeAtTableRow(table.getSelectedRow());
    }

    /**
     * Gets a list of all the selected nodes in the JTable, or an empty list if no rows are selected.
     * @return a list of all the selected nodes in the JTable, or an empty list if no rows are selected.
     */
    public List<TreeNode> getSelectedNodes() {
        List<TreeNode> nodes = new ArrayList<>(table.getSelectedRowCount());
        if (table != null) {
            for (int rowIndex : table.getSelectedRows()) {
                nodes.add(getNodeAtTableRow(rowIndex));
            }
        }
        return nodes;
    }

    /**
     * Gets the index in the model of a row in a bound table.
     * If the table is unsorted, the model and table indexes will be identical.
     * If the table is sorted, then the row sorter can provide a mapping from the row in the visible table
     * to the row in this underlying model.
     *
     * @param tableRow The row in the JTable to get the model index for.
     * @return the model index of a row in a bound JTable.
     */
    public int getModelIndexForTableRow(final int tableRow) {
        final RowSorter<? extends TableModel> rowSorter = table == null? null : table.getRowSorter();
        return rowSorter == null? tableRow : rowSorter.convertRowIndexToModel(tableRow);
    }

    /**
     * Gets the index in the model of a node, or -1 if it isn't visible or part of the current tree.
     *
     * @param node The node to get the model index for.
     * @return The index in the model of displayed nodes, or -1 if it isn't being displayed.
     */
    public int getModelIndexForTreeNode(final TreeNode node) {
        return displayedNodes.size() < linearScanThreshold ? getModelIndexLinearScan(node) : getModelIndexTreeScan(node);
    }

    /**
     * Gets the index in the model of a node, or -1 if it isn't visible or part of the current tree.
     * Uses a simple linear scan of the visible node array list to find the node.
     *
     * @param node The node to get the model index for.
     * @return The index in the model of displayed nodes, or -1 if it isn't being displayed.
     */
    protected int getModelIndexLinearScan(final TreeNode node) {
        return displayedNodes.indexOf(node);
    }

    /**
     * Gets the index in the model of a node, or -1 if it isn't visible or part of the current tree.
     * Uses a tree scanning algorithm, walking back up the path of the node we're looking for and
     * locating each ancestor, adding up how many visible nodes precede it, until we reach the
     * node we want to find, or not.
     * <p>
     * This is quite fast since we already track how many visible child nodes (including all sub nodes) each expanded
     * folder contains.  So we can just walk the children of each ancestor adding up how many nodes will actually appear,
     * and don't have to actually scan most of them. This will be a considerably smaller set of nodes than looking in all of them.
     *
     * @param node The node to get the model index for.
     * @return The index in the model of displayed nodes, or -1 if it isn't being displayed.
     */
    protected int getModelIndexTreeScan(final TreeNode node) {
        // Definitely not in tree if the node we're looking for is filtered.
        if (isFiltered(node)) {
            return NOT_LOCATED;
        }

        // If it's the root node and not filtered (due to above test), it's only visible if showing root.
        if (node == rootNode) {
            return showRoot? 0 : NOT_LOCATED;
        }

        // Walk up the path from the root up to the node we want to calculate the index for,
        // calculating the model index as the sum of all visible children that precede it in the path.
        final List<TreeNode> parentPath = buildPath(node); // last item is the root, first is the node to find.
        final TreeNode ancestorRoot = parentPath.get(parentPath.size() - 1);
        CALCULATE_INDEX:
        if (ancestorRoot == rootNode) { // If we're in the same tree (starts at the same root):

            //TODO: think more about filtering the root node when it's both showing and not showing...

            /*
             * Set the starting model index for the count of nodes.
             * If the root is showing, and is unfiltered, it has index zero in the model, so adding the first child would be at 1.  Start count at 0.
             * If the root isn't showing or is filtered, start at -1, so adding a single child to it would give index of zero.
             */
            int modelIndex = showRoot && !isFiltered(rootNode) ? 0 : -1;

            // For each parent/child pair in the path starting from root and working upwards towards the node we want the model index for,
            // add up all the visible child nodes that precede our path in the tree.
            // The path is guaranteed to have at least two entries at this point -  the node itself and root,
            // as we know it isn't the root node due to the first tests in this method.
            // Start the loop at the second to last item in the path (child of root), since we need the parent of the child to find
            // as well (we work in parent/child pairs), which will be the last item in the path (root).  Root has no parent, so we can't start there.
            final int childOfRootIndex = parentPath.size() - 2;
            for (int pathIndex = childOfRootIndex; pathIndex >= 0; pathIndex--) {
                final TreeNode parentNode = parentPath.get(pathIndex + 1); // Get the parent of the one we will find.  This is root initially.

                // As long as the parent node is expanded and unfiltered (so children will be visible)
                if (isExpanded(parentNode) && !isFiltered(parentNode)) {
                    final TreeNode childToFind = parentPath.get(pathIndex);

                    // Add up all the visible children of that parent which precede the child we want to find.
                    final int visibleChildren = addUpVisibleChildren(parentNode, childToFind);
                    if (visibleChildren == NOT_LOCATED) {
                        /*
                         *If we do not end up finding the child node in the parent in the tree, this means that there is
                        * is either a bug in the tree nodes (it has parents down to root, but one of the parents
                        * doesn't recognise one of them as a child), or a bug in this algorithm.
                        * //TODO: should we throw an error?  return not found?
                         */
                        break CALCULATE_INDEX; // stop looking and fall through to return NOT_LOCATED.
                    }

                    // Increase the calculated model index by the number of nodes above us in the path to that point.
                    modelIndex += visibleChildren;
                } else {
                    break CALCULATE_INDEX; // stop looking and fall through to return NOT_LOCATED.
                }
            }
            return modelIndex;
        }
        return NOT_LOCATED;
    }

    /**
     * Returns the number of visible nodes in the child of a parent that precede a child we are trying to find,
     * or -1 if the child does not exist in that parent.
     *
     * @param parentNode The parent node to scan.
     * @param nodeToFind The child node to find in the parent.
     * @return the number of visible child nodes up to the child to find, or -1 if the child does not exist.
     */
    protected int addUpVisibleChildren(TreeNode parentNode, TreeNode nodeToFind) {
        boolean located = false;
        int visibleNodeCount = 0;
        for (int child = 0; child < parentNode.getChildCount(); child++) {
            final TreeNode childNode = parentNode.getChildAt(child);
            if (!isFiltered(childNode)) {
                visibleNodeCount++; // add one for the child.

                // Have we found an ancestor parent node?
                if (childNode == nodeToFind) {
                    located = true;
                    break; // stop looking for more at this level, move up the ancestor path to the next level.
                }

                // Add how many nodes this child has as visible children and sub-children.
                // This is just a lookup in a hash map at worst, so it is fast.
                visibleNodeCount += getExpandedChildCount(childNode);
            }
        }
        return located ? visibleNodeCount : NOT_LOCATED;
    }

    protected List<TreeNode> buildPath(final TreeNode node) {
        final List<TreeNode> path = new ArrayList<>();
        TreeNode currentNode = node;
        while (currentNode != null ) {
            path.add(currentNode);
            currentNode = currentNode.getParent();
        }
        return path;
    }


    /* *****************************************************************************************************************
     *                                           Event listeners
     */

    /**
     * Adds a listener to TreeTableEvents, which inform the listener when a node is about to expand or collapse.
     *
     * @param listener The listener to be notified of tree expand or collapse events.
     */
    public void addExpandCollapseListener(final ExpandCollapseListener listener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }

    /**
     * Removes a TreeTableEvent listener.
     *
     * @param listener The listener to remove.
     */
    public void removeExpandCollapseListener(final ExpandCollapseListener listener) {
        eventListeners.remove(listener);
    }

    /**
     * Adds a mouse listener for expand / collapse events to a JTable
     **/
    protected void addMouseListener() {
        if (table != null) {
            tableMouseListener = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    toggleExpansion(e);
                }
            };
            table.addMouseListener(tableMouseListener);
        }
    }

    /**
     * Removes a mouse listener for expand / collapse events
     * from a JTable previously registered by a call to addMouseListener().
     *
     */
    protected void removeMouseListener() {
        if (table != null) {
            table.removeMouseListener(tableMouseListener);
        }
        tableMouseListener = null;
    }

    /**
     * Adds keyboard actions for + (expand), - (collapse) and space (toggle) selected node expansion.
     */
    protected void addKeyboardActions() {
        if (table != null) {
            final InputMap inputMap = table.getInputMap(JComponent.WHEN_FOCUSED);
            for (KeyStroke keyStroke : expandKeys) {
                inputMap.put(keyStroke, EXPAND_NODE_KEYSTROKE_KEY);
            }
            for (KeyStroke keyStroke : collapseKeys) {
                inputMap.put(keyStroke, COLLAPSE_NODE_KEYSTROKE_KEY);
            }
            for (KeyStroke keyStroke : toggleKeys) {
                inputMap.put(keyStroke, TOGGLE_EXPAND_NODE_KEYSTROKE_KEY);
            }
            final ActionMap actionMap = table.getActionMap();
            actionMap.put(EXPAND_NODE_KEYSTROKE_KEY, new TreeTableExpandAction());
            actionMap.put(COLLAPSE_NODE_KEYSTROKE_KEY, new TreeTableCollapseAction());
            actionMap.put(TOGGLE_EXPAND_NODE_KEYSTROKE_KEY, new TreeTableToggleExpandAction());
        }
    }

    /**
     * Removes keyboard actions for + (expand), - (collapse) and space (toggle) selected node expansion.
     */
    protected void removeKeyboardActions() {
        if (table != null) {
            final InputMap inputMap = table.getInputMap(JComponent.WHEN_FOCUSED);
            for (KeyStroke keyStroke : expandKeys) {
                inputMap.remove(keyStroke);
            }
            for (KeyStroke keyStroke : collapseKeys) {
                inputMap.remove(keyStroke);
            }
            for (KeyStroke keyStroke : toggleKeys) {
                inputMap.remove(keyStroke);
            }
            final ActionMap actionMap = table.getActionMap();
            actionMap.remove(EXPAND_NODE_KEYSTROKE_KEY);
            actionMap.remove(COLLAPSE_NODE_KEYSTROKE_KEY);
            actionMap.remove(TOGGLE_EXPAND_NODE_KEYSTROKE_KEY);
        }
    }

    /**
     * Sets the click handler for the primary tree column.  If not set explicitly, it will either default
     * to the primary TableCellRenderer if it implements the TreeClickHandler interface, or a very rough default
     * handler will be used that estimates the likely node indent for a tree when processing clicks.
     *
     * @param clickHandler The TreeClickHandler to process expand/collapse events for the primary tree column.
     */
    public void setTreeClickHandler(final TreeClickHandler clickHandler) {
        this.clickHandler = clickHandler; //TODO: what happens if this is updated after binding to a tree...?
    }


    /* *****************************************************************************************************************
     *                                          Column methods
     */

    /**
     * Returns the table column model telling JTable what columns to display and their order.
     *
     * It calls createTableModel() if one has not yet been set, which is an abstract method which subclasses must implement.
     * It will also automatically set a TreeCellRenderer on the TableColumn with model index zero, if a renderer is not
     * already set on it.
     *
     * @return The TableColumnModel telling JTable what columns to display and their order.
     */
    public TableColumnModel getTableColumnModel() {
        if (columnModel == null) {
            columnModel = createTableColumnModel();
            setTreeCellRendererOnFirstColumnIfNotSet(columnModel);
        }
        return columnModel;
    }

    /**
     * Scans through the columns in the TableColumnModel looking for the column with a model index of 0
     * (TREE_COLUMN_INDEX).  If this column does not have a CellRenderer set on it, the default
     * TreeCellRenderer will be set.  The column with a model index of 0 is always the column
     * which renders the tree and responds to expand or collapse clicks.
     *
     * @param columnModel The TableColumnModel to scan.
     */
    protected void setTreeCellRendererOnFirstColumnIfNotSet(final TableColumnModel columnModel) {
        for (int columnIndex = 0; columnIndex < columnModel.getColumnCount(); columnIndex++) {
            TableColumn column = columnModel.getColumn(columnIndex);
            if (column.getModelIndex() == TREE_COLUMN_INDEX && column.getCellRenderer() == null) {
                column.setCellRenderer(new TreeCellRenderer(this));
            }
        }
    }

    /**
     * Utility method to simplify creating columns for subclasses.
     * Defaults to having no cell renderer or cell editor specified - JTable has default renderers and editors for simple data types.
     * The tree column at model index 0 will automatically get a TreeCellRenderer if it isn't specified.
     * @param modelIndex The model index of the column.  0 is always the tree rendering column.
     * @param headerValue The header value
     * @return a TableColumn with the values provided and defaults for the others.
     */
    protected TableColumn createColumn(final int modelIndex, final Object headerValue) {
        return createColumn(modelIndex, headerValue, DEFAULT_COLUMN_WIDTH, null, null);
    }

    /**
     * Utility method to simplify creating columns for subclasses.
     * Defaults to having no cell renderer or cell editor specified - JTable has default renderers and editors for simple data types.
     * The tree column at model index 0 will automatically get a TreeCellRenderer if it isn't specified.     *
     * @param modelIndex The model index of the column.  0 is always the tree rendering column.
     * @param headerValue The header value
     * @param width The width of the column.
     * @return a TableColumn with the values provided and defaults for the others.
     */
    protected TableColumn createColumn(final int modelIndex, final Object headerValue, final int width) {
        return createColumn(modelIndex, headerValue, width, null, null);
    }

    /**
     * Utility method to simplify creating columns for subclasses.
     * Defaults to having no cell editor specified - JTable has default editors for simple data types.
     * If specifying a cell renderer for model index 0, it must be capable of rendering the tree structure.
     * @param modelIndex The model index of the column.  0 is always the tree rendering column.
     * @param headerValue The header value
     * @param cellRenderer The TableCellRenderer to use with the data types in that column.
     * @return a TableColumn with the values provided and defaults for the others.
     */
    protected TableColumn createColumn(final int modelIndex, final Object headerValue,
                                       final TableCellRenderer cellRenderer) {
        return createColumn(modelIndex, headerValue, DEFAULT_COLUMN_WIDTH, cellRenderer, null);
    }

    /**
     * Utility method to simplify creating columns for subclasses.
     * Defaults to having no cell editor specified - JTable has default editors for simple data types.
     * If specifying a cell renderer for model index 0, it must be capable of rendering the tree structure.     *
     * @param modelIndex The model index of the column.  0 is always the tree rendering column.
     * @param headerValue The header value
     * @param width The width of the column.
     * @param cellRenderer The TableCellRenderer to use with the data types in that column.
     * @return a TableColumn with the values provided and defaults for the others.
     */
    protected TableColumn createColumn(final int modelIndex, final Object headerValue, final int width,
                                       final TableCellRenderer cellRenderer) {
        return createColumn(modelIndex, headerValue, width, cellRenderer, null);
    }

    /**
     * Utility method to simplify creating columns for subclasses.
     *
     * @param modelIndex The model index of the column.  0 is always the tree rendering column.
     * @param headerValue The header value
     * @param cellRenderer The TableCellRenderer to use with the data types in that column.
     * @param cellEditor The TableCellEditor to use with the data types in that column.
     * @return a TableColumn with the values provided, and a default width.
     */
    protected TableColumn createColumn(final int modelIndex, final Object headerValue,
                                       final TableCellRenderer cellRenderer, final TableCellEditor cellEditor) {
        return createColumn(modelIndex, headerValue, DEFAULT_COLUMN_WIDTH, cellRenderer, cellEditor);
    }

    /**
     * Utility method to simplify creating columns for subclasses.
     *
     * @param modelIndex The model index of the column.  0 is always the tree rendering column.
     * @param headerValue The header value
     * @param width The width of the column.
     * @param cellRenderer The TableCellRenderer to use with the data types in that column.
     * @param cellEditor The TableCellEditor to use with the data types in that column.
     * @return a TableColumn with the values provided.
     */
    protected TableColumn createColumn(final int modelIndex,  final Object headerValue, final int width,
                                       final TableCellRenderer cellRenderer, final TableCellEditor cellEditor) {
        final TableColumn column = new TableColumn(modelIndex, width, cellRenderer, cellEditor);
        column.setHeaderValue(headerValue);
        return column;
    }


    /* *****************************************************************************************************************
     *                                Node expansion and collapse and tree change.
     */

    /**
     * @param node The node to check.
     * @return true if the node is expanded.
     */
    public boolean isExpanded(final TreeNode node) {
        return expandedNodeCounts.containsKey(node);
    }

    /**
     * @return a read only set of tree nodes which are currently expanded.  This will change as expanded nodes change.
     */
    public Set<TreeNode> getExpandedNodes() {
        return expandedNodeCounts.keySet();
    }

    //TODO: Possible BUG.
    //      this method is named wrong - it doesn't return visible child counts - it returns expanded node counts.
    //      these will only be valid for nodes that are actually visible.
    //      You can have invisible expanded nodes (if some earlier parent is collapsed)
    //      You can have filtered expanded nodes (if the node or some earlier).
    //      So this method is really "get

    //      If expanded node counts are reset or removed for invisible

    //      Does setting a filter change old expanded node counts if re-expanding a parent?


    //TODO: finish refactoring this method - what does it return?  what does it return for nodes which are expanded,
    //      but which are not visible?   Does the expandedNodeCount return 0 for expanded nodes which aren't visible (except root if hidden)?
    //      Filtering will force a rebuild of the tree anyway... what about nodes which were expanded but not currently visible?  Should reset to 0?
    //      Alternatively, should it accurately re-calculate what the visible expanded counts of all nodes are, even non-visible ones?
    //      Node structure change also forces a refresh of either the whole tree, or part of it.  Again, what happens to previously expanded nodes under it?
    /**
     * Returns the number of children which would be //TODO: would be expanded?  Are expanded?  Expanded but not visible?
     *
     * //TODO: difference between number of actually visible expanded nodes, and nodes which are expanded but not currently visible.
     *
     * @param parentNode The node to get a visible child count for.
     * @return The number of visible children under this node (including any other expanded nodes underneath).
     */
    public int getExpandedChildCount(final TreeNode parentNode) {
        /*
         * This method must not depend on any current properties of the node beyond returning what we know about how
         * many visible children it has.  The node itself and its children may have changed when this method is called.
         * Had a bug where I had optimised the lookup by only doing it if the node had children.  But the node
         * can have had its children removed, even though we're still displaying them in the tree and counting them.
         */
        final Integer childCount = expandedNodeCounts.get(parentNode);
        return childCount == null ? 0 : childCount;
    }

    /**
     * Sets a node to expand, if it isn't already expanded and allows children.
     * Even if the node has no children but allows children, it will still be set to be expanded.
     * Any children added to it would automatically be visible once expanded.
     *
     * @param node The node to expand.
     */
    public void expandNode(final TreeNode node) {
        if (!isExpanded(node)) {
            toggleExpansion(node, getModelIndexForTreeNode(node));
        }
    }

    /**
     * Expands all nodes in the tree which allow children.
     */
    public void expandTree() {
        expandChildren(rootNode);
    }

    /**
     * Expands all nodes in the tree which allow children up to the maximum depth specified.
     * A depth of one expands the root only, showing the children of the root.  A depth of 2 would also
     * expand those children, showing 2 levels of node under the root.
     *
     * @param depth The number of levels of children to make visible under the root.
     */
    public void expandTree(final int depth) {
        expandChildren(rootNode, depth);
    }

    /**
     * Expands all nodes in the tree which allow children and also pass the node predicate test.
     * Any child nodes of a node which doesn't pass the test will not be expanded further, but
     * siblings of that node and their child nodes may be expanded.
     *
     * @param nodePredicate The predicate a node must pass in order to be expanded.
     */
    public void expandTree(final Predicate<TreeNode> nodePredicate) {
        expandChildren(rootNode, nodePredicate);
    }

    /**
     * Expands all nodes in the tree which allow children up to the maximum depth specified and which pass
     * the node predicate test. Any child nodes of a node which doesn't pass the test will not be expanded further,
     * but siblings of that node and their child nodes may be expanded.
     *
     * A depth of one expands the root only, showing the children of the root.  A depth of 2 would also
     * expand those children, showing 2 levels of node under the root.
     *
     * @param depth The number of levels of children to make visible under the root.
     * @param nodePredicate The predicate a node must pass in order to be expanded.
     */
    public void expandTree(final int depth, final Predicate<TreeNode> nodePredicate) {
        expandChildren(rootNode, depth, nodePredicate);
    }

    /**
     * Expands the parent node and all children and subchildren nodes which allow children.
     *
     * @param parentNode The node to expand and all children and subchildren.
     */
    public void expandChildren(final TreeNode parentNode) {
        expandNode(parentNode);
        for (int childIndex = 0; childIndex < parentNode.getChildCount(); childIndex++) {
            expandChildren(parentNode.getChildAt(childIndex));
        }
    }

    /**
     * Expands the parent node and all children and subchildren if they meet the node predicate test.
     * No further children will be expanded for a node that fails the predicate test,
     * but other siblings of it that pass the test may be expanded.
     *
     * @param parentNode The node to expand along with children
     * @param nodePredicate The predicate a node must pass in order to expand itself and its children.
     */
    public void expandChildren(final TreeNode parentNode, final Predicate<TreeNode> nodePredicate) {
        if (nodePredicate.test(parentNode)) {
            expandNode(parentNode);
            for (int childIndex = 0; childIndex < parentNode.getChildCount(); childIndex++) {
                expandChildren(parentNode.getChildAt(childIndex), nodePredicate);
            }
        }
    }

    /**
     * Expands the parent node and its children, and sub-children up to the depth (number of levels of sub-children)
     * from the parent.  A depth of one gives the immediate children of the parent, 2 gives their children as well,
     * and so on.
     *
     * @param parentNode The parent node to expand along with its children and sub-children up to the depth specified.
     * @param depth The maximum depth to expand a parent node, 1 being its immediate children, 2 being their children and so on.
     */
    public void expandChildren(final TreeNode parentNode, final int depth) {
        if (depth > 0) { // as long as there's a depth level to expand...
            expandNode(parentNode);
            if (depth > 1) { // don't bother trying to expand children if they wouldn't be expanded anyway due to depth.
                for (int childIndex = 0; childIndex < parentNode.getChildCount(); childIndex++) {
                    expandChildren(parentNode.getChildAt(childIndex), depth - 1);
                }
            }
        }
    }

    /**
     * Expands the parent node and all children and subchildren if they meet the node predicate test, up to the
     * maximum depth of children specified.  No further children will be expanded for a node that fails the predicate test,
     * but other siblings of it that pass the test may be expanded.
     *
     * @param parentNode The parent node to expand along with its children and sub-children up to the depth specified.
     * @param depth The maximum depth to expand a parent node, 1 being its immediate children, 2 being their children and so on.
     * @param nodePredicate The predicate a node must pass in order to expand itself and its children.
     */
    public void expandChildren(final TreeNode parentNode, final int depth, final Predicate<TreeNode> nodePredicate) {
        if (depth > 0 && nodePredicate.test(parentNode)) { // as long as there's a depth level to expand and the node passes the test...
            expandNode(parentNode);
            if (depth > 1) { // don't bother trying to expand children if they wouldn't be expanded anyway due to depth.
                for (int childIndex = 0; childIndex < parentNode.getChildCount(); childIndex++) {
                    expandChildren(parentNode.getChildAt(childIndex), depth - 1, nodePredicate);
                }
            }
        }
    }

    /**
     * Collapses a node if it is already expanded.
     * @param node The node to collapse.
     */
    public void collapseNode(final TreeNode node) {
        if (isExpanded(node)) {
            toggleExpansion(node, getModelIndexForTreeNode(node));
        }
    }

    /**
     * Collapse all expansions in the tree.
     * If the root is not showing, it will be expanded (or nothing in the tree could be visible).
     */
    public void collapseTree() {
        clearExpansions();
        fireTableDataChanged();
    }

    /**
     * Collapses expansion of the parent node and all children (and sub children) of a parent node.
     *
     * @param parentNode The node to collapse all children.
     */
    public void collapseChildren(final TreeNode parentNode) {
        collapseNode(parentNode);
        for (int childIndex = 0; childIndex < parentNode.getChildCount(); childIndex++) {
            collapseChildren(parentNode.getChildAt(childIndex));
        }
    }

    /**
     * Collapses expansion of the parent node andy any children that meet the node predicate.
     * It will not continue collapsing children of any node that fails the predicate.
     *
     * @param parentNode The node and all children to collapse that meet the node predicate
     * @param nodePredicate The predicate a node must meet to collapse.
     */
    public void collapseChildren(final TreeNode parentNode, final Predicate<TreeNode> nodePredicate) {
        if (nodePredicate.test(parentNode)) {
            collapseNode(parentNode);
            for (int childIndex = 0; childIndex < parentNode.getChildCount(); childIndex++) {
                collapseChildren(parentNode.getChildAt(childIndex));
            }
        }
    }

    /**
     * Listeners may change the node structure (e.g. dynamically add, remove or change nodes).
     * If we're going to remove nodes (currently expanded), we need to get the number of nodes that are
     * *currently* visible, before any listeners run, so we know how many current nodes to remove, and
     * whether the node was already expanded or not.
     *
     * @param node The node to toggle expansion.
     * @param modelIndex The row in the model the node exists at.
     */
    protected void toggleExpansion(final TreeNode node, final int modelIndex) {
        final boolean currentlyExpanded = isExpanded(node);
        if (node.getAllowsChildren()) {
            if (listenersApprove(node, currentlyExpanded)) {
                if (expansionChangeAffectsVisibleNodes(node, modelIndex)) {
                    toggleVisibleExpansion(node, modelIndex, currentlyExpanded); // deal with changes to visible nodes.
                } else {
                    toggleInvisibleExpansion(node, currentlyExpanded); // node not visible - just toggle it's expanded state.
                }
            }
        }
    }

    /**
     * @param node The node to expand or collapse.
     * @param modelIndex A model index of the node in the visible tree, or negative if it isn't currently visible.
     * @return true if expanding or collapsing the node may affect visible rows in the tree.
     */
    protected boolean expansionChangeAffectsVisibleNodes(final TreeNode node, final int modelIndex) {
        return (modelIndex >= 0 && modelIndex < displayedNodes.size()) || // it has a valid visible index
               (!showRoot && node == rootNode);                        // or it's a hidden root node.
    }

    /**
     * Toggles the expansion of a visible node in the tree, removes any old nodes and adds new ones back in.
     *
     * @param parentNode The visible node to toggle expansion.
     * @param parentModelIndex The model row of the visible node.  Must be the model row of the node.
     * @param currentlyExpanded Whether the node is currently expanded.
     */
    protected void toggleVisibleExpansion(final TreeNode parentNode, final int parentModelIndex, final boolean currentlyExpanded) {
        final int childrenChanged;
        if (currentlyExpanded) {
            childrenChanged = getExpandedChildCount(parentNode);
            removeVisibleRows(parentModelIndex + 1, childrenChanged);
            expandedNodeCounts.remove(parentNode);
            updateTreeChildCounts(parentNode.getParent(), -childrenChanged);
        } else {
            childrenChanged = addVisibleChildren(parentModelIndex, parentNode); //TODO: check model row of add visible children.
            expandedNodeCounts.put(parentNode, childrenChanged);
            updateTreeChildCounts(parentNode.getParent(), childrenChanged);
        }

        // If we haven't removed or added any children, notify the table this one node may have changed.
        // This forces a visual refresh which may alter the rendering of the expand or collapse handles.
        if (childrenChanged == 0) {
            fireTableRowsUpdated(parentModelIndex, parentModelIndex);
        }
    }

    /**
     * Toggles expansion of a node which isn't currently visible, so no nodes to remove or add from the visible tree.
     *
     * @param node The node to toggle expansion.
     * @param currentlyExpanded Whether the node is currently expanded.
     */
    protected void toggleInvisibleExpansion(final TreeNode node, final boolean currentlyExpanded) {
        if (currentlyExpanded) {
            expandedNodeCounts.remove(node);
        } else { //TODO: do we care about invisible node counts?
            expandedNodeCounts.put(node, calculateVisibleChildNodes(node, true));
        }
    }

    /**
     * Updates the visible child count for a node with a delta change (negative or positive),
     * and updates all of its parents with the adjusted delta.
     * Must only be called for a node which is actually visible in the tree.
     *
     * @param node The node whose child count is changing by some negative or positive number.
     * @param delta The change in the child count for the node.
     */
    protected void updateVisibleChildCounts(final TreeNode node, final int delta) {
        if (delta != 0) {
            final int existingChildren = getExpandedChildCount(node);
            final int newVisibleChildren = existingChildren + delta;
            expandedNodeCounts.put(node, newVisibleChildren);
            updateTreeChildCounts(node.getParent(), newVisibleChildren);
        }
    }

    /**
     * Updates all child counts down the tree to root given a starting node and the change in child counts,
     * which can be negative or positive.
     *
     * @param startNode The node to start adjusting child counts for, down to the root node.
     * @param delta The amount to adjust the child counts, negative or positive.
     */
    protected void updateTreeChildCounts(final TreeNode startNode, final int delta) {
        if (delta != 0) {
            final Map<TreeNode, Integer> nodeCounts = expandedNodeCounts;
            TreeNode currentNode = startNode;
            // Propagate child counts down the tree, as long as the parents themselves are expanded.
            // A collapsed parent should remain collapsed, and no further updates should be propagated downwards.
            while (currentNode != null && isExpanded(currentNode)) {
                final int nodeCount = nodeCounts.get(currentNode); // can't be null, because it is expanded.
                nodeCounts.put(currentNode, nodeCount + delta);
                currentNode = currentNode.getParent();
            }
        }
    }

    /**
     * Clears the node expansion records for all nodes.
     * If the root node is not showing, then the root will be re-expanded (as otherwise nothing can be visible in the tree).
     */
    protected void clearExpansions() {
        expandedNodeCounts.clear();
        if (!showRoot) { // expand the root if it's not showing - or nothing will ever be visible in the tree!
            expandNode(rootNode);
        }
    }

    /**
     * Toggles expansion of nodes based on a mouse click.
     *
     * @param evt The mouse event to process.
     */
    protected void toggleExpansion(final MouseEvent evt) {
        if (table != null) {
            final Point point = evt.getPoint();
            final int tableRow = table.rowAtPoint(point);
            final int columnIndex = table.columnAtPoint(point);
            final int modelRow = getModelIndexForTableRow(tableRow);
            final TreeNode node = displayedNodes.get(modelRow);
            if (getClickHandler().clickOnExpand(node, columnIndex, evt)) {
                toggleExpansion(node, modelRow);
            }
        }
    }


    /* *****************************************************************************************************************
     *                                         Visible node management.
     */

    /**
     * @return true if the root node will be displayed, false if not.
     */
    public boolean getShowRoot() {
        return showRoot;
    }

    /**
     * Sets whether the root node should be displayed or not, and updates the display model if the state changes.
     *
     * @param showRoot whether the root node should be displayed or not.
     */
    public void setShowRoot(final boolean showRoot) {
        if (this.showRoot != showRoot) {
            toggleShowRoot();
        }
    }

    /**
     * Toggles whether the root node is showing in the tree.
     */
    public void toggleShowRoot() {
        this.showRoot = !showRoot; // toggle root status.
        if (isFiltered(rootNode)) { // if root node is filtered (whether showing or not), refresh the tree if we change it's stauts.
            refreshTree(false);
            fireTableDataChanged();
        } else { // add or remove the root node if the root isn't filtered.
            if (showRoot) {
                displayedNodes.add(0, rootNode);
                fireTableRowsInserted(0, 0);
            } else {
                displayedNodes.remove(0);
                fireTableRowsDeleted(0, 0);
            }
        }
    }

    /**
     * @param node The node to check.
     * @return true if the node is currently visible in the tree.
     */
    public boolean isVisible(final TreeNode node) {
        if (node == rootNode) {
            return showRoot && !isFiltered(node); // root only visible if showing and not filtered.
        }
        return node != null && !isFiltered(node) && parentsAreVisibleAndPartOfTree(node);
    }

    /**
     * @return An unmodifiable list view of all visible nodes in the model.
     */
    public List<TreeNode> getVisibleNodes() {
        return Collections.unmodifiableList(displayedNodes);
    }

    /**
     * Rebuilds the entire visible tree from the root.
     * Does not alter any prior node expansions, so they will remain expanded when the
     * tree is rebuilt.
     */
    protected void buildVisibleNodes() {
        displayedNodes.clear();
        if (!isFiltered(rootNode)) {
            if (showRoot) {
                displayedNodes.add(rootNode);
            }
            buildVisibleChildren(rootNode, displayedNodes);
        }
    }

    /**
     * Removes rows from the visible display and notifies the table of the removal.
     *
     * @param firstRowToRemove The model row index to start removing.
     * @param numberToRemove the number of model rows to remove.
     */
    protected void removeVisibleRows(final int firstRowToRemove, final int numberToRemove) {
        if (numberToRemove > 0) {
            final int lastChildRow = firstRowToRemove + numberToRemove - 1;
            // Removing children which are displayed doesn't have to deal with filtering, since if they are visible,
            // they are, by definition, not filtered in the first place.
            if (numberToRemove == 1) {
                displayedNodes.remove(firstRowToRemove);
            } else {
                displayedNodes.remove(firstRowToRemove, lastChildRow);
            }
            fireTableRowsDeleted(firstRowToRemove, lastChildRow);
        }
    }

    /**
     * Adds the children of the parent node to the displayed node list, and notifies the table of the insertion.
     * @param parentModelRow The model index of the parent node.
     * @param parentNode The parent node whose children should be added.
     * @return The number of new child nodes added.
     */
    protected int addVisibleChildren(final int parentModelRow, final TreeNode parentNode) {
        final int childCount = parentNode.getChildCount();
        if (childCount > 0) {
            final int firstChildRow = parentModelRow + 1;
            // Build visible children will filter out any filtered nodes, so we don't have to deal with filtering in this method.
            final List<TreeNode> newVisibleNodes = buildVisibleChildren(parentNode);
            final int newChildren = newVisibleNodes.size();
            if (newChildren == 1) {
                displayedNodes.add(firstChildRow, newVisibleNodes.get(0));
            } else {
                displayedNodes.addAll(firstChildRow, newVisibleNodes);
            }
            fireTableRowsInserted(firstChildRow, firstChildRow + newChildren - 1);
            return newChildren;
        }
        return 0;
    }

    /**
     * Builds a list of nodes which would be visible from the node given.
     * The node given does not yet have to be set as expanded in the model,
     * but any children must already be expanded if their children will be built.
     *
     * @param node The node to build its children and their visible children for.
     * @return A list of TreeNodes which are the visible children of that node and their children.
     */
    protected List<TreeNode> buildVisibleChildren(final TreeNode node) {
        if (node.getChildCount() > 0) {
            final List<TreeNode> children = new ArrayList<>(128);
            int totalVisibleChildren = 0;
            for (int childIndex = 0; childIndex < node.getChildCount(); childIndex++) {
                final TreeNode child = node.getChildAt(childIndex);
                if (!isFiltered(child)) {
                    children.add(child);
                    totalVisibleChildren += buildVisibleChildren(node.getChildAt(childIndex), children);
                }
            }
            // Reset the child count for each expanded node as it is built (some nodes may be filtered or it may have changed entirely since last rebuild).
            return children;
        }
        return Collections.emptyList();
    }

    /**
     * Builds a list of children which are visible from the node given and adds them to an existing\ List.
     * The node given must already be expanded in the model for its visible children to be built.
     *
     * @param parentNode The node to build visible children for.
     * @param visibleNodes A list of TreeNodes which are the visible children of that node and their children.
     */
    protected int buildVisibleChildren(final TreeNode parentNode, final List<TreeNode> visibleNodes) {
        int totalVisibleChildren = 0;
        if (isExpanded(parentNode)) { // if expanded, then add in the visible children.
            for (int childIndex = 0; childIndex < parentNode.getChildCount(); childIndex++) {
                final TreeNode child = parentNode.getChildAt(childIndex);
                if (!isFiltered(child)) { //TODO: this leaves old "expanded" nodes under currently filtered nodes with incorrect expanded node values, since we halt processing whena node is filtered, rather than expanded.
                    visibleNodes.add(child);
                    totalVisibleChildren += (1 + buildVisibleChildren(child, visibleNodes)); // And this child and the children of the child if they're visible...
                }
            }
            // Reset the child count for each expanded node as it is built (some nodes may be filtered or it may have changed entirely since last rebuild).
            expandedNodeCounts.put(parentNode, totalVisibleChildren); // ensure child counts for any expanded nodes are updated when the tree is rebuilt.
        }
        return totalVisibleChildren;
    }

    /**
     * Calculates how many child nodes will be visible given a parent node.
     * If you want a count of the visible nodes of a parent before it is actually expanded,
     * you can say to expand the parent (no matter what it's actual expanded status right now).
     *
     * @param node The node to get a count of visible children for.
     * @param expandNode true if you want a count no matter whether it is currently expanded or not.
     * @return A count of all children which would be visible in this parent.
     */
    protected int calculateVisibleChildNodes(final TreeNode node, boolean expandNode) {
        int totalVisibleChildNodes = 0;
        if (expandNode || isExpanded(node)) {
            for (int childIndex = 0; childIndex < node.getChildCount(); childIndex++) {
                final TreeNode child = node.getChildAt(childIndex);
                totalVisibleChildNodes += (1 + calculateVisibleChildNodes(child, false));
            }
        }
        return totalVisibleChildNodes;
    }

    /**
     * @param node The node to check.
     * @return true if the node is in a visible chain of unfiltered, expanded parents and in the same tree as this model.
     */
    protected boolean parentsAreVisibleAndPartOfTree(final TreeNode node) {
        TreeNode parentNode, currentNode = node;
        while ((parentNode = currentNode.getParent()) != null) {
            if (!isExpanded(parentNode) || isFiltered(parentNode)) {
                return false;
            }
            currentNode = parentNode;
        }
        // All parents are expanded if we get to a root, AND the root IS the tree root node.
        // If we have a node with a null parent which isn't the root,
        // then it's not part of this tree and thus cannot be visible in it.
        return currentNode == rootNode && !isFiltered(rootNode);
    }


    /* *****************************************************************************************************************
     *                                            Listener management
     */

    protected boolean listenersApprove(final TreeNode node, final boolean isCollapsing) {
        return isCollapsing ? listenersApprovedCollapseEvent(node) : listenersApprovedExpandEvent(node);
    }

    protected boolean listenersApprovedExpandEvent(final TreeNode node) {
        for (ExpandCollapseListener listener : eventListeners) {
            if (!listener.nodeExpanding(node)) {
                return false;
            }
        }
        return true;
    }

    protected boolean listenersApprovedCollapseEvent(final TreeNode node) {
        for (ExpandCollapseListener listener : eventListeners) {
            if (!listener.nodeCollapsing(node)) {
                return false;
            }
        }
        return true;
    }

    protected TreeClickHandler getClickHandler() {
        if (clickHandler == null) {
            clickHandler = getOrCreateTreeClickHandler();
        }
        return clickHandler;
    }

    protected TreeClickHandler getOrCreateTreeClickHandler() {
        final TableColumn column = getColumnWithModelIndex(TREE_COLUMN_INDEX);
        if (column != null) {
            TableCellRenderer renderer = column.getCellRenderer();
            if (renderer instanceof TreeClickHandler) {
                return (TreeClickHandler) renderer;
            }
        }
        return new TreeCellRenderer(this); // set a default click handler.
    }

    protected TableColumn getColumnWithModelIndex(final int modelIndex) {
        TableColumnModel model = getTableColumnModel();
        for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++) {
            final TableColumn column = model.getColumn(columnIndex);
            if (column.getModelIndex() == modelIndex) {
                return column;
            }
        }
        return null;
    }


    /* *****************************************************************************************************************
     *                             Key bindings for expand and collapse and toggle.
     */

    /**
     * Sets the key strokes used to expand a selected node in the tree.
     * If null or empty, no key strokes will be assigned.
     *
     * @param newExpandKeys The new set of keystrokes which will trigger an expand event on a selected node.
     */
    public void setExpandKeys(final KeyStroke... newExpandKeys) {
        final KeyStroke[] newStrokes = newExpandKeys == null? new KeyStroke[0] : expandKeys.clone();
        removeKeyboardActions();
        this.expandKeys = newStrokes;
        addKeyboardActions();
    }

    /**
     * Sets the key strokes used to collapse a selected node in the tree.
     * If null or empty, no key strokes will be assigned.
     *
     * @param newCollapseKeys The new set of keystrokes which will trigger a collapse event on a selected node.
     */
    public void setCollapseKeys(final KeyStroke... newCollapseKeys) {
        final KeyStroke[] newStrokes = newCollapseKeys == null? new KeyStroke[0] : newCollapseKeys.clone();
        removeKeyboardActions();
        this.collapseKeys = newStrokes;
        addKeyboardActions();
    }

    /**
     * Sets the keystrokes used to toggle expansion of a selected node in the tree.
     * If null or empty, no key strokes will be assigned.
     *
     * @param newToggleKeys The new set of keystrokes which will trigger a toggle expansion event on a selected node.
     */
    public void setToggleKeys(final KeyStroke... newToggleKeys) {
        final KeyStroke[] newStrokes = newToggleKeys == null? new KeyStroke[0] : newToggleKeys.clone();
        removeKeyboardActions();
        this.toggleKeys = newStrokes;
        addKeyboardActions();
    }

    /**
     * @return An array of KeyStrokes which will trigger an expansion event on the selected node.
     */
    public KeyStroke[] getExpandKeys() {
        return expandKeys;
    }

    /**
     * @return An array of KeyStrokes which will trigger a collapse event on the selected node.
     */
    public KeyStroke[] getCollapseKeys() {
        return collapseKeys;
    }

    /**
     * @return An array of KeyStrokes which will trigger a toggle expansion event on the selected node.
     */
    public KeyStroke[] getToggleKeys() {
        return toggleKeys;
    }

    /* *****************************************************************************************************************
     *                                     General methods
     */

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "(root node = " + rootNode +
                ", show root = " + showRoot +
                ", size = " + displayedNodes.size() +
                ", table = " + table +
                ", filtering = " + isFiltering() +
                ", sorting = " + isSorting() +
                ", grouping = " + isGrouping() + ')';
    }

    /* *****************************************************************************************************************
     *                            Action classes to bind to keyboard events.
     */

    protected class TreeTableExpandAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source instanceof JTable) {
                final JTable table = (JTable) source;
                final int modelIndexRow = getModelIndexForTableRow(table.getSelectedRow());
                final TreeNode node = getNodeAtModelIndex(modelIndexRow);
                if (node != null && node.getAllowsChildren() && !isExpanded(node)) {
                    toggleExpansion(node, modelIndexRow);
                }
            }
        }
    }

    protected class TreeTableCollapseAction  extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source instanceof JTable) {
                final JTable table = (JTable) source;
                final int modelIndexRow = getModelIndexForTableRow(table.getSelectedRow());
                final TreeNode node = getNodeAtModelIndex(modelIndexRow);
                if (node != null && node.getAllowsChildren() && isExpanded(node)) {
                    toggleExpansion(node, modelIndexRow);
                }
            }
        }
    }

    protected class TreeTableToggleExpandAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source instanceof JTable) {
                final JTable table = (JTable) source;
                final int modelIndexRow = getModelIndexForTableRow(table.getSelectedRow());
                final TreeNode node = getNodeAtModelIndex(modelIndexRow);
                if (node != null) {
                    toggleExpansion(node, modelIndexRow);
                }
            }
        }
    }


    /* *****************************************************************************************************************
     *                                              Interfaces
     */

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
        boolean clickOnExpand(TreeNode node, int column, MouseEvent evt);
    }

    /**
     * An interface for a listener to expand or collapse events in the tree.
     */
    public interface ExpandCollapseListener {

        /**
         * Notifies the listener that the tree node will expand.
         * @param node the node that is about to expand
         * @return true if you want to allow the expansion.
         */
        boolean nodeExpanding(TreeNode node);

        /**
         * Notifies the listener that the tree node will collapse.
         * @param node the node that is about to collapse
         * @return true if you want to allow the collapse.
         */
        boolean nodeCollapsing(TreeNode node);
    }



}
