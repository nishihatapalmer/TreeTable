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
package net.byteseek.demo.treetable;

import java.util.Comparator;
import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.TreeNode;
import net.byteseek.swing.treetable.Comparators;
import net.byteseek.swing.treetable.TreeTableModel;
import net.byteseek.swing.treetable.TreeUtils;

public final class MyObjectTreeTableModel extends TreeTableModel {

    private Icon leafIcon;              // tree node that allows no children.
    private Icon openIcon;              // tree node displaying children.
    private Icon closedIcon;            // tree node not displaying children.

    public MyObjectTreeTableModel(final TreeNode rootNode, final boolean showRoot) {
        super(rootNode, showRoot);
        setIcons();
        setGroupingComparator(Comparators.ALLOWS_CHILDREN);
        leafIcon = null;
    }

    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return columnIndex < 3; // can't edit the "num children" column as that is calculated, not stored.
    }

    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        switch (columnIndex) {
            case 0: return String.class;
            case 1: return Long.class;
            case 2: return Boolean.class;
            case 3: return Integer.class;
        }
        return Object.class;
    }

    @Override
    public Object getColumnValue(final TreeNode node, final int column) {
        final MyObject obj = TreeUtils.getUserObject(node);
        switch (column) {
            case 0: return obj.getDescription();
            case 1: return obj.getSize();
            case 2: return obj.isEnabled();
            case 3: return node.getChildCount();
            default: return null;
        }

    }

    @Override
    public TableColumnModel createTableColumnModel() {
        TableColumnModel result = new DefaultTableColumnModel();
        result.addColumn(TreeUtils.createColumn(0, "description"));
        result.addColumn(TreeUtils.createColumn(1, "size"));
        result.addColumn(TreeUtils.createColumn(2, "enabled"));
        result.addColumn(TreeUtils.createColumn(3, "children"));
        return result;
    }

    @Override
    public void setColumnValue(final TreeNode node, final int column, final Object value) {
        final Object o = TreeUtils.getUserObject(node);
        if (o instanceof MyObject) {
            final MyObject obj = (MyObject) o;
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
    }

    @Override
    public Comparator<?> getColumnComparator(int column) {
        return null; //TODO: test return special comparator for column values to enable sorting with that comparator.
    }

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

    private void setIcons() {
        if (UIManager.getLookAndFeel().getID().equals("GTK")) {
            setLeafIcon(UIManager.getIcon("FileView.fileIcon"));
            setOpenIcon(UIManager.getIcon("FileView.directoryIcon"));
            setClosedIcon(UIManager.getIcon("FileView.directoryIcon"));
        } else {
            // Leaf, open and closed icons not available in all look and feels...not in GTK, but is in metal...
            setLeafIcon(UIManager.getIcon("Tree.leafIcon"));
            setOpenIcon(UIManager.getIcon("Tree.openIcon"));
            setClosedIcon(UIManager.getIcon("Tree.closedIcon"));
        }
    }

    public void setLeafIcon(final Icon leafIcon) {
        this.leafIcon = leafIcon;
    }

    public void setClosedIcon(final Icon closedIcon) {
        this.closedIcon = closedIcon;
    }

    public void setOpenIcon(final Icon openIcon) {
        this.openIcon = openIcon;
    }


}
