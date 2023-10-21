# TreeTable

TreeTable displays a tree with additional columns for each item in the tree, in a standard Java Swing `JTable`.  

![TreeTable](treetable.png)

## Reasons to use TreeTable
Unlike many other TreeTable components, the tree can be composed of any type of `TreeNode`, and you can use any `TreeModel` with it.  The table is a standard `JTable` and supports everything you can do with that component.  It does not require the use of special nodes or custom components; it tracks the properties of the tree internally using efficient algorithms.

It provides sorting on multiple columns, filtering, and grouping sibling nodes together (e.g. folders and files).  Nodes can be expanded, collapsed or toggled programmatically, by mouse click or by customisable key bindings.  

All JTable features work as normal, including cell rendering and cell editing.  A custom table header is provided which shows the number of the sorted column in a multi-column sort, but you can use a standard or custom table header if you prefer.

### Software status
This software is in **beta**.  It is feature complete.  Some tests still need to be written.  The overall code shouldn't change too much, but methods may be refactored.
The demo code will probably be extended a bit and the help expanded.

### Integrating the code
The `TreeTable` component has not yet been published to maven, as it is still in beta.  Therefore, you should clone the repository and add the source code directly to your own project.  Once the code is at a release status, it will be published to maven, and then you can reference it in your build dependencies (e.g. pom file).

### Extending the code
Parts of the `TreeTableModel` are designed to be overridden in order to provide required mappings to your objects and to set icons.  These methods are documented.

Most of the rest of the TreeTable code can also be overridden if you need to, but be very careful if you do.
If you override methods in the `TreeTableModel`, you should generally call the base implementation using `super`.  
## Getting started
This section covers how to get started with using TreeTableModel in your own code.  

### The object
We'll start with the object whose values you want to display as columns in a tree table.  For example:

```java
   public class MyObject {
       private String description;
       private Long size;
       private Boolean enabled;
       // ... rest of class definition.
   }
       
```

### The model
First we need to extend a `TreeTableModel`, to provide the mapping between `MyObject` fields and the columns in the table, and to define the columns.

All the examples assume your tree is composed of `DefaultMutableTreeNode` objects, and its `MyObject` is stored in the user object property of the tree node:

```java
public class MyObjectTreeTableModel extends TreeTableModel {
    
    public MyObjectTreeTableModel(TreeNode rootNode, boolean showRoot) {
        super(rootNode, showRoot);
    }

    @Override
    public Object getColumnValue(TreeNode node, int column) {
        MyObject myObject = TreeUtils.getUserObject(node);
        switch (column) {
            case 0: return myObject.getDescription();
            case 1: return myObject.getSize();
            case 2: return myObject.getEnabled();
        }
        return null;
    }

    @Override
    public TableColumnModel createTableColumnModel() {
        TableColumnModel result = new DefaultTableColumnModel();
        result.addColumn(createColumn(0, "description"));
        result.addColumn(createColumn(1, "size"));
        result.addColumn(createColumn(2, "enabled"));
        return result;
    }
}
```

The `TableColumn` with column model index 0 renders the tree and responds to mouse clicks to expand or collapse nodes.

In the example above, the tree will be rendered in the first column, which is probably what you want most of the time.  You can add the columns in any order you like, or use the `moveColumn()` method after the columns are added.  No matter how the columns are arranged, the one with model index 0 is the tree.

### Displaying the tree
To display the tree, bind your `MyObjectTreeTableModel` to a standard `JTable` with a root node for the tree.

```java
   JTable table = // your JTable 
   MyObject rootObject  = new MyObject("First root", 10000L, true);
   TreeNode rootNode    = new DefaultMutableTreeNode(rootObject);
   TreeTableModel model = new MyObjectTreeTableModel(rootNode);
   model.bindTable(table);
```

By default, the root node is visible in the tree.  You can change whether the root node is showing on construction or by using `setShowRoot(boolean)` at any time.  When the root node is not showing, it will be expanded by default so its immediate children will be visible.

## Building a tree

In the example above, we only built a single root node, so only a single row would be displayed in the table.  You must create the `TreeNode` structure for your tree.  

If your objects already have a tree structure, you can build a mirrored tree of `DefaultMutableTreeNode` from them using the static utility method `TreeUtils.buildTree()`.  You must supply the root object of the tree, and a lambda, or instance of `ChildProvider`, that returns a list of child objects from a parent object. For example, if MyObject has a method `getChildren()` that returns a list of MyObject children:

```java
   MyObject rootObject = // your root object.
   MutableTreeNode rootNode = TreeUtils.buildTree(rootObject, parent -> parent.getChildren());
```

You can build a tree of `TreeNode` by any other means before a `TreeTableModel` is constructed with the root `TreeNode`.  After construction, the model needs to be kept in sync with the tree if it changes, or you can dynamically build the tree nodes by responding to expand or collapse events.

## Expanding and collapsing nodes
Even after building a larger tree, only the root, or its immediate children if the root is not showing, will be visible.  This is because nodes with children are unexpanded by default.

### Mouse clicks
Nodes can be expanded or collapsed by clicking to the left of the expand/collapse handle in the tree column.

### Keystrokes
Selected nodes can be expanded or collapsed with key presses, defaulting to using the `+` key for expand, the `-` key for collapse, and the space key to toggle between expand and collapse.  

You can change which KeyStrokes are defined for these events using `setExpandKeys()` , `setCollapseKeys()` and `setToggleKeys()`. You can set no keystrokes if you want no keyboard controls.

### Programmatically
You can control the expansion or collapse of nodes programmatically, by calling `expandNode`, `collapseNode` or `toggleNode`:

```java
    TreeNode node = // some node we want to expand or collapse.
    treeTableModel.expandNode(node);
    treeTableModel.collapseNode(node);
    treeTableModel.toggleNode(node);
```

There are also methods which will expand or collapse all the children and sub-children of nodes to some depth, or the entire tree, and control whether they expand or collapse using a `Predicate`.

## Tree navigation key bindings
You can navigate the tree using key presses, in the same way by default as a `JTree`.  The left arrow key will either collapse the current node, or move back to its parent node if already collapsed.
The right arrow key will either expand the current node, or move to its first child if already expanded.  

You can change which KeyStrokes are defined for these events using `setNavigateParentKeys()` and `setNavigateChildrenKeys()`.  You can also change whether the current node is expanded or collapsed when navigating by calling `setCollapseOnNavigateToParent()` and `setExpandOnNavigateToChildren()`.

Other key navigation methods as the same as for `JTable`: up arrow and down arrow move up and down rows, home moves to the start of the tree, end moves to the end, and page up and page down move up or down a page.  These are not defined in the `TreeTableModel` as they are already part of the `JTable` key navigation.  To change these, you must alter them on the `JTable`.



## Updating the tree
Once a tree is built, if you make changes to the tree nodes, the model needs to stay in sync with the tree if it changes.

### Build on expand
When a node expands or collapses, the model fires a `TreeTableEvent`, which you can subscribe to. A listener is allowed to make direct modifications to the tree structure for the node being processed and any part of the subtree it contains.  It must not modify other areas of the tree.

```java
model.addExpandCollapseListener(new TreeTableModel.ExpandCollapseListener() {
    @Override
    public boolean nodeExpanding(TreeNode node) {
        yourMethodToAddChildrenToNode(node);
        return true;
    }

    @Override
    public boolean nodeCollapsing(TreeNode node) {
        ((DefaultMutableTreeNode) node).removeAllChildren();
        return true;
    }
});
```

Any changes made to a subtree inside the `ExpandCollapseListener` will automatically be processed by the `TreeTableModel`.  If you want to cancel an expansion or collapse event, return `false` from the event method.

In the example above, tree node children are being removed on a collapse event.  You don't have to remove child nodes on collapse, but it is fine to do so.  Most of the time you probably don't want to unless you are very memory constrained.

#### Removing expand and collapse handles for nodes with no children.
Expand and collapse handles are rendered for all `TreeNode` objects which allow children.  If on dynamic expand there are no child nodes to be added, you can set the node to not allow children.  For example, assuming `DefaultMutableTreeNode` is being used:

```java
    @Override
    public boolean nodeExpanding(TreeNode node) {
        yourMethodToAddChildrenToNode(node);
        if (node.getChildCount() == 0) {
            ((DefaultMutableTreeNode) node).setAllowsChildren(false);
        }
        return true;
    }
```

### Using a TreeModel
If you use a standard `TreeModel` to manage insertions and removals of nodes in your tree, then the `TreeTableModel` can register as a listener for `TreeModelEvents` and update itself automatically as the tree is altered.

```java
    // Initialize the TreeTableModel, TreeModel and register for TreeModelEvents
    TreeTableModel treeTableModel = new MyObjectTreeTableModel(rootNode);
    DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
    treeModel.addTreeModelListener(treeTableModel);
       
    // Use the TreeModel to update the tree. The TreeTableModel will update itself automotically.
    MutableTreeNode newChild = new DefaultMutableTreeNode(newChildObject);       
    treeModel.insertNodeInto(newChild, parentNode, 0);
```

Technically, there is no dependence at all on `TreeModel`.  Any object that fires the appropriate `TreeModelEvents` and can register `TreeModelListeners` will be fine.  It doesn't have to be a `TreeModel`, although it's most likely to be a `DefaultTreeModel` since that is a standard class that just works.

### Manual change notification
If you don't use something that fires `TreeModelEvents` then you can manually call the appropriate update methods on the `TreeTableModel` after you make changes to the tree.

```java
   // Insert a new child node at index 0
   MutableTreeNode newChild = new DefaultMutableTreeNode(newChildObject);    
   parentNode.insert(newChild, 0);
   treeTableModel.treeNodeInserted(parentNode, 0);
   
   // Remove a child node
   parentNode.remove(childToRemove);
   treeTableModel.treeNodeRemoved(parentNode, childToRemove);
```

You don't need to call these methods if changes are made inside an `ExpandCollapseListener`.

## Rendering
All rendering of the table cells is performed using standard `TableCellRenderer` objects.  

### Icons
To supply icons for nodes in the tree column, override `TreeTableModel.getNodeIcon(TreeNode)` in your `TreeTableModel`, and implement whatever logic you require to supply the correct icon for the node.  If no icon exists, it is safe to return a null Icon.  For example:

```java
    @Override
    public Icon getNodeIcon(TreeNode node) {
        if (node != null) {
            if (node.getAllowsChildren()) {
                return isExpanded(node) ? openIcon : closedIcon;
            }
            return leafIcon;
        }
        return null;
    }
```

### Tree renderer
By default, the tree column (with column model index 0) automatically uses a `TreeTableCellRenderer`, if you don't specify otherwise.  This will pick up the icon for it by calling `treeTableModel.getNodeIcon(TreeNode)`.

You can use a different renderer (or a subclass) if you prefer, by specifying the cell renderer to use when you create the TableColumns.  The `TreeTableCellRenderer` provides overridable methods to allow setting various properties as you wish without having to re-implement most of it.

If implementing a completely different tree renderer, you should also implement a `TreeClickHandler` which determines whether a click in this column is an expand or collapse event.  Then set the `TreeClickHandler` using `model.setTreeClickHandler()`.  In general, you probably don't want to implement your own tree renderer and click handler from scratch, unless there is something particularly unusual. 

### Other columns
If you do not specify a `TableCellRenderer` for each `TableColumn`, then the `JTable` will just use a default `Object` renderer for those columns, which just calls `toString()`.  To make the `JTable` use better renderers appropriate to the type of the column, you can override the `TreeTableModel.getColumnClass()` method:

```java
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0: return String.class;
            case 1: return Long.class;
            case 2: return Boolean.class;
        }
        return Object.class;
    }
```

`JTable` has built-in `TableCellRenderers` for `Object` (including `String`), `Boolean`, `Number`, `Float`, `Double`, `Icon`, `IconImage` and `Date`.   

If you need more specialised renderers for different data types, you must specify them in the `TableColumn`.  You do not have to return a `Class` for a column in `getColumnClass(int)` if you have already specified a `TableCellRenderer` in a `TableColumn` itself.  Only columns for which you want to use the built-in `JTable` renderers need to return a `Class` in this method, although it is good practice to default to returning `Object.class` for all others.

### Table headers
By default, a `TreeTableHeaderRenderer` is used to render the table header.  This displays multi-column sorts by adding the number of the sort column as well as the ascending/descending icons.

You can use a different header renderer if you like - just use one of the `bind()`  methods that lets you specify an alternative header renderer, or set it yourself on the `JTable` directly.  The header renderer has no knowledge that there is a tree being rendered.

## Sorting
By default, sorting is enabled with a multi-column sort up to three columns. You can disable sorting by setting a null `RowSorter` on the `JTable`, or by calling `model.setSortEnabled(false)` 

### Column sorting
For each column that sorting is defined on, the following comparators will be used on the column values in this precedence order:
1. Custom comparator, if defined.
2. Compared directly, if they implement `Comparable` and are the same `Class`.
3. Compared on their string values.

Custom comparators can be supplied for any column by overriding `TreeTableModel.getColumnComparator(int)`.  

### Sort behaviour
By default, when a column header is clicked on, if the column is already being sorted, it will flip from ascending to descending to unsorted.  If it's a new column, it will be added as an additional ascending column, up to three maximum. 

This behaviour can be fully customised by creating an appropriate `ColumnSortStrategy` object and setting it on the model.

```java
   // Make a new strategy with 5 maximum sort columns and different behaviour:
   ColumnSortStrategy strategy = new TreeTableColumnSortStrategy(5, 
        TreeTableColumnSortStrategy.ToggleNewColumnAction.MAKE_FIRST,
        TreeTableColumnSortStrategy.ToggleExistingColumnAction.MAKE_FIRST,
        TreeTableColumnSortStrategy.WhenUnsortedAction.REMOVE_SUBSEQUENT);

   // Set it on the model.
   treeTableModel.setColumnSortStrategy(strategy);
```

### Maintaining a sorted state
If you always want the tree to be sorted, you can define default sort keys.  The default sort keys are used by the `TreeTableRowSorter` if all other columns become unsorted, ensuring the table is always in a sorted state.

```java
   model.setDefaultSortKeys(new RowSorter.SortKey(0, SortOrder.ASCENDING));
```

## Grouping
If you want to group sibling nodes by some feature of a node or its user object, you can set a node comparator that implements `Comparator<TreeNode>` by calling `model.setGroupingComparator(comparator)`.

For example, some nodes represent files and some folders, and you'd like all the folders to be grouped together, and all the files, with column sorting within those groups.  There is a comparator defined in `TreeUtils` which does this:
```java
    model.setGroupingComparator(TreeUtils.GROUP_BY_ALLOWS_CHILDREN);
```
If a grouping comparator is set, sibling nodes will always be grouped by the comparator, even if no other columns are being sorted on.  You can remove grouping by setting a null grouping comparator or calling `clearGroupingComparator()`.

## Filtering
You can filter nodes from the visible tree by specifying a `Predicate<TreeNode>`.  Any node that passes the predicate test will be filtered out from display.   For example, to filter out all the nodes that don't allow children:

```java
    model.setNodeFilter(treeNode -> !treeNode.getAllowsChildren());
```

You can turn off filtering by setting a null filter, or by calling `clearNodeFilter()`, and you can determine if you are currently filtering by calling `isFiltering()`.


## Editing
If you want to edit cells in the tree table, you have to override  `TreeTableModel.isCellEditable()` and return `true` if a particular cell is editable.
```java
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }
```
You must also override `TreeTableModel.setColumnValue()` to write to the correct field for a column that's editable:
```java
 @Override
    public void setColumnValue(TreeNode node, int column, Object value) {
        MyObject obj = TreeUtils.getUserObject(node);
        switch (column) {
            case 0: {      
                obj.setDescription((String) value);
                break;
            }
            case 1: {
                obj.setSize((Long) value);
                break;
            }
            case 2: {
                obj.setEnabled((Boolean) value);
                break;
            }
        }
    }
```


If you are not specifying `TableCellEditor` objects in each `TableColumn`, you must override `TreeTableModel.getColumnClass()` to return the types of the objects in the column.  
```java
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0: return String.class;
            case 1: return Long.class;
            case 2: return Boolean.class;
        }
        return Object.class;
    }

```
`JTable` has default cell editors for `String`, `Boolean`, and `Number` only.  If you want to edit any other data types, you must implement your own `TableCellEditor` and set it on the appropriate `TableColumn`.