package test;

import net.byteseek.swing.treetable.TreeTableModel;

import javax.swing.tree.DefaultMutableTreeNode;

public class TestTreeTableModel extends TreeTableModel {

    public TestTreeTableModel(DefaultMutableTreeNode rootNode, boolean showRoot) {
        super(rootNode, showRoot);
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    protected Object getObjectColumnValue(final Object o, final int column) {
        if (o instanceof TestClass) {
            TestClass obj = (TestClass) o;
            switch (column) {
                case 0: return obj.getDescription();
                case 1: return obj.getSize();
                case 2: return obj.isEnabled();
            }
        }
        return null;
    }

    protected String getHeaderName(final int column) {
        switch (column) {
            case 0: return "description";
            case 1: return "size";
            case 2: return "enabled";
            default: return "";
        }
    }


}
