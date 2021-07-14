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
