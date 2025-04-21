package net.byteseek.demo.treetable;

import net.byteseek.swing.treetable.TableUtils;
import net.byteseek.swing.treetable.TreeTableModelObjectArray;
import net.byteseek.swing.treetable.TreeUtils;

import javax.swing.table.JTableHeader;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

/**
 * The CSVTree class provides functionality for reading hierarchical data from a CSV file,
 * constructing a tree structure, and displaying the tree in a graphical JTable-based viewer.
 * It includes methods for parsing CSV data, creating tree nodes, and rendering the tree as a GUI.
 * <p>
 * It also replaces the default mouse click behaviour on the headers, so that it will sort for each header click,
 * unless the cursor is a resize cursor.  In that case, if it is a double-click, it will resize the column to fit
 * the contents.
 */
public class CSVTree {

    /**
     * Reads CSV data and constructs a tree structure based on specified ID and parent ID columns,
     * then displays the tree.
     *
     * @param reader             The reader to read data from
     * @param idColumnName       The name of the column representing the unique identifier for each row.
     * @param parentIdColumnName The name of the column representing the parent identifier for each row.
     * @param separatorChar      The character used to separate columns in the data.
     */
    public static void readAndDisplayTree(BufferedReader reader, String idColumnName, String parentIdColumnName, char separatorChar) throws IOException{
        // Get the CSV headers and the indexes of the id and parent id columns:
        TreeUtils.CSVTableHeaderInfo headerInfo =
                TreeUtils.processCSVHeaders(reader, idColumnName, parentIdColumnName, separatorChar);

        // Create an iterator for the table tree data:
        TreeUtils.CSVTreeTableRowIterator rowIterator =
                new TreeUtils.CSVTreeTableRowIterator(reader,
                        headerInfo.getIdColumnIndex(),
                        headerInfo.getParentIdColumnIndex(), separatorChar);

        // Build the tree from the rows:
        DefaultMutableTreeNode rootNode = TreeUtils.buildTree(rowIterator);

        // Display the tree
        displayTree(rootNode, headerInfo.getHeaders());
    }

    /**
     * Displays a Swing form containing a JTable bound to a TreeTableModelObjectArray.
     * The table column model is generated using TreeUtils.buildTableColumnModel() from the headers.
     *
     * @param rootNode The root node of the tree structure.
     * @param headers  An array of column headers for the table.
     */
    public static void displayTree(DefaultMutableTreeNode rootNode, Object[] headers) throws IOException {
        javax.swing.SwingUtilities.invokeLater(() -> {
            // Build the column model
            javax.swing.table.TableColumnModel columnModel = TableUtils.buildTableColumnModel(Arrays.asList(headers));

            // Create a TreeTableModelObjectArray from the root node and column model.
            TreeTableModelObjectArray treeTableModel = new TreeTableModelObjectArray(rootNode, columnModel);
            treeTableModel.setShowRoot(false);

            // Create a JTable and set the TreeTableModelObjectArray as its model
            javax.swing.JTable table = new javax.swing.JTable();
            treeTableModel.bindTable(table);

            // Replace the default look and feel mouse click listener so it sorts on every click,
            // and resizes the column on a double-click to fit the contents, if the cursor is a resize cursor.
            TableUtils.AWTMouseListenerReplacer clickReplacer = new TableUtils.AWTMouseListenerReplacer(
                    new TableUtils.HeaderSortResizeMouseClickListener(), MouseEvent.MOUSE_CLICKED, table.getTableHeader());
            clickReplacer.activate();

            // Place the table in a scroll pane
            javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(table);

            // Create a JFrame to display the table
            javax.swing.JFrame frame = new javax.swing.JFrame("CSV Tree Viewer");
            frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
            frame.add(scrollPane);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
    
    /**
     * The entry point of the CSVTree program. This method reads a CSV file, parses the data,
     * and builds a tree structure based on the specified ID and parent ID columns.
     *
     * @param args The command-line arguments. Expected arguments are:
     *             args[0] - The name of the CSV file to read.
     *             args[1] - The name of the column representing the unique ID for each row.
     *             args[2] - The name of the column representing the parent ID for each row.
     *             args[3] - The character to use as a separator, or the 2-digit hex value of the character to use.
     *                       If nothing is provided, it will default to a comma.
     */
    public static void main(String[] args) {
        if (args.length < 3 || args.length > 4) {
            System.err.println("Usage: java CSVTree <fileName> <idColumnName> <parentIdColumnName> [<separatorChar>]");
            return;
        }
        char separatorChar = args.length == 3 ? ',' : args[3].length() == 1 ? args[3].charAt(0) : (char) Integer.parseInt(args[3], 16);
        try (BufferedReader reader = new BufferedReader(new FileReader(args[0]))) {
            readAndDisplayTree(reader, args[1], args[2], separatorChar);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
