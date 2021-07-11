# TreeTable

Treetable is a set of Java classes that displays a tree of information with additional columns for each item in the tree in a standard `JTable`.

## The object
Let's say we have defined an object that records information about people and who they manage, and we want to display this in a tree table:
```java
   public class Person {
       private String name;
       private String role;
       private Person[] reports;
       ...
   }
       
```

## The model
First we need to subclass a `TreeTableModel`, which defines how to map a table to the `Person` class.  At a minimum, three abstract methods need to be defined, which define the table columns that the `JTable` will use, and get and set values given a column index.
```java
   public class PersonTreeTableModel extends TreeTableModel {
   
      public Object getColumnValue(TreeTableNode node, int column) {  
         Person person = (Person) node.getUserObject();
         switch (column) {
            case 0: return person.getName();
            case 1: return person.getRole();
         }
         return null;
      }
    
      public void setColumnValue(TreeTableNode node, int column, Object value) {
         Person person = (Person) node.getUserObject();
         switch (column) {
            case 0: person.setName((String) value);
            case 1: person.setRole((String) role);
         }
      }
    
      public TableColumn getTableColumn(int column) {
         switch (column) {
            case 0: return createColumn("Name", 0, new TreeTableCellRenderer(this));
            case 1: return createColumn("Role", 1, null);
         }
         throw new IllegalArgumentException("Column index: " + column + " must be 0 or 1");
      }
      
   }
```
Note that we provide a `TreeTableCellRenderer` for the first column.  This is a built-in renderer that will render the tree structure.
One column needs to provide a `TreeTableCellRenderer` (or another class that can render the tree structure).


## Displaying the tree
To display a `TreeTableModel`, bind it to a standard `JTable`:

```java
   JTable table = ... 
   Person person = new Person("Joe Bloggs", "Chief of Everything");
   TreeTableNode root = new TreeTableNode(person, true);
   TreeTableModel model = new PersonTreeTableModel(root, true);
   model.bindTable(table);
```
