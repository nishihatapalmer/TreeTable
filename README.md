# TreeTable

Treetable displays a tree of information, with additional columns for each item in the tree, in a standard Java Swing `JTable`.

### Software status
This software is still in **alpha**.  It may change unexpectedly at any time.
The parts that are implemented seem to work OK, but we have no unit tests, and there are still some features unimplemented (e.g. editing cells).


## Getting started

### The object
Let's say we are working with data about people who manage other people.  We'd like to display this as a tree of people with their names and roles in separate columns.

```java
   public class Person {
       private String name;
       private String role;
       private List<Person> reports;
       ...
   }
       
```

### The model
First we need to subclass a `TreeTableModel`, which defines how to map a table to the `Person` class.  

```java
   public class PersonTreeTableModel extends TreeTableModel {
   
      private static final int NUM_COLUMNS = 2;
   
      public PersonTreeTableModel(TreeTableNode rootNode, boolean showRoot) {
          super(rootNode, NUM_COLUMNS, showRoot);
      }
   
      public Object getColumnValue(TreeTableNode node, int column) {  
          Person person = (Person) node.getUserObject();
          switch (column) {
             case 0: return person.getName();
             case 1: return person.getRole();
          }
         throw new IllegalArgumentException("Invalid column: " + column);
      }
    
      public void setColumnValue(TreeTableNode node, int column, Object value) {
          Person person = (Person) node.getUserObject();
          switch (column) {
            case 0: {
                person.setName((String) value);
                break;
            }
            case 1: {
                person.setRole((String) value);
                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid column: " + column);
            }
         }
      }
    
      public TableColumn getTableColumn(int column) {
          switch (column) {
             case 0: return createColumn(0, "Name");
             case 1: return createColumn(1, "Role");
          }
          throw new IllegalArgumentException("No column exists for " + column);
      }
      
   }
```

### Displaying the tree
To display a `TreeTableModel`, instantiate a model with a root node, and bind it to a standard `JTable`:

```java
   JTable table = ... 
   Person person = new Person("Joe Bloggs", "Chief of Everything");
   TreeTableNode root = new TreeTableNode(person, true);
   TreeTableModel model = new PersonTreeTableModel(root, true);
   model.bindTable(table);
```


## Building a tree

The `TreeTableModel` **does not** build the tree nodes for you.  You must create nodes and child nodes using `TreeTableNode` or a subclass, and assign the correct user objects to the nodes. 

### Dynamically building a tree
You can dynamically build nodes on expand, or remove them on collapse, by implementing the `TreeTableEvent.Listener` and responding to expand or collapse events.

If on dynamic expand there are no child nodes to be added, you can set the node to not allow children `node.setAllowsChildren(false)` in the tree event.  The node will no longer display expand or collapse handles.

## The tree column
The tree column is the column in which the tree structure is rendered.

### Expanding and collapsing nodes
Selected nodes can be expanded or collapsed by clicking to the left of the expand handle, or via the keyboard with the `+` and `-` keys.  The keys to use are configurable by calling `model.setExpandChar()` and `model.setCollapseChar()`.  You can set them both to be the same char if you prefer, e.g. space bar toggles expand/collapse.

When a node expands or collapses, it fires a `TreeTableEvent`, which you can subscribe to.  A listener has the option of cancelling the event.  It is allowed to make modifications to the tree structure for the node being processed and any of its children.  It must not modify other areas of the tree.

### Icons
To supply icons for nodes in the tree column, override `TreeTableModel.getNodeIcon(node)`. 

### Column

The column which renders the tree defaults to the first column.  This can be changed by calling `model.setTreeColumn(columnIndex)` to the model index of the tree column in which the tree should appear.


### Renderer
By default, the tree column uses a `TreeTableCellRenderer`.  You can use a different renderer (or a subclass) if you prefer, by specifying the renderer to use when you create the TableColumns in the `model.getTableColumn()` method.  TableColumns let you set the cell renderer and the cell editor to use for that column.

If implementing a different tree renderer, you should also implement a `TreeClickHandler` which determines whether a click in this column is an expand or collapse event.  Then set the `TreeClickHandler` using `model.setTreeClickHandler()`

In general, you probably don't want to implement your own tree renderer from scratch, unless there is something particularly unusual.  You can easily subclass the `TreeTableCellRenderer` to add different formatting, and return your subclassed renderer in the appropriate TableColumn.

## Table headers
By default, a `TreeTableHeaderRenderer` is used to render the table header.  This displays multi-column sorts by adding the number of the sort column as well as the ascending/descending icons.

You can use a different header renderer if you like - just use one of the `bind()`  methods that lets you specify an alternative header renderer, or set it yourself on the `JTable` directly.  The header renderer has no knowledge that there is a tree being rendered.

## Sorting
Sorting is enabled by default, giving a multi-column sort (up to three columns). 

### Column sorting
For each column that sorting is defined on, the following comparators will be used on the column values in this precedence order:
1. Custom comparator, if defined.
2. Compared directly, if they implement `Comparable`.
3. Compared on their string values.

Custom comparators can be supplied for any column by overriding `TreeTableModel.getColumnComparator()`.

### Grouping
If you want to group nodes by some feature of a node user object that isn't a column value, you can set a node comparator that implements `Comparator<TreeTableNode>` by calling `model.setNodeComparator(comparator)`.  

For example, some nodes represent files and some folders, and you'd like all the folders to be grouped together, and all the files, with column sorting within those groups.  This can be achieved by setting a node comparator that makes folders "smaller than" files.

If a node comparator is set, nodes will always be grouped by the comparator, even if no other columns are being sorted on.