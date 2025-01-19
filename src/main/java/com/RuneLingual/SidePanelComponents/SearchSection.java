package com.RuneLingual.SidePanelComponents;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.RuneLingual.LangCodeSelectableList;
import com.RuneLingual.SQL.*;
import com.RuneLingual.RuneLingualPlugin;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchSection extends JPanel {
    private JTextField englishInput;
    private JTextField foreignInput;
    private JButton englishSearchButton;
    private JButton foreignSearchButton;
    private JList<String> englishResults;
    private JList<String> foreignResults;
    private SidePanel sidePanel;
    private DefaultTableModel tableModelEn;
    private JTable enTable;
    private DefaultTableModel tableModelForeign;
    private JTable foreignTable;
    private RuneLingualPlugin plugin;

    String textSearchEnglish = "Search English name";
    String textSearchForeign = "Search Foreign name";
    String textSearch = "Search";
    String textResult = "Result";
    String textType = "Type";

    public SearchSection(SidePanel sideP, LangCodeSelectableList langList, RuneLingualPlugin plugin){
        this.sidePanel = sideP;
        this.plugin = plugin;

        translatePanelTexts(langList);

        englishInput = new JTextField();
        englishInput.setSize(200, 25);
        foreignInput = new JTextField();
        foreignInput.setSize(200, 25);

        englishSearchButton = new JButton(textSearchEnglish);
        englishSearchButton.setBackground(Color.darkGray);
        englishSearchButton.setSize(200, 25);
        foreignSearchButton = new JButton(textSearchForeign);
        foreignSearchButton.setBackground(Color.darkGray);
        foreignSearchButton.setSize(200, 25);

        sidePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        sidePanel.add(createTextLabel(textSearchEnglish));
        sidePanel.add(englishInput);
        sidePanel.add(englishSearchButton);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 1)));
        enTable = addEnResultTable();
        tableModelEn = (DefaultTableModel) enTable.getModel();

        sidePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        sidePanel.add(createTextLabel(textSearchForeign));
        sidePanel.add(foreignInput);
        sidePanel.add(foreignSearchButton);
        sidePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        foreignTable = addForeignResultTable();
        tableModelForeign = (DefaultTableModel) foreignTable.getModel();


        englishSearchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeAllRows(tableModelEn);
                String searchQuery = getExactMatchQuery(englishInput.getText(), SqlVariables.columnEnglish.getColumnName());
                String searchAlikeQuery = getAlikeQuery(englishInput.getText(), SqlVariables.columnEnglish.getColumnName());

                //log.info("Search Query: " + searchQuery);
                SqlActions sqlActions = new SqlActions(plugin);
                String[][] results = sqlActions.executeSearchQuery(searchQuery);
                String[][] resultsAlike = sqlActions.executeSearchQuery(searchAlikeQuery);
                //log.info("Results: " + results.length);
                for(String[] result : results){
                    tableModelEn.addRow(new String[]{result[0], result[1], result[2]});
                }
                for(String[] result : resultsAlike){
                    tableModelEn.addRow(new String[]{result[0], result[1], result[2]});
                }

                // adjust column width
                for (int i = 0; i < tableModelEn.getColumnCount(); i++) {
                    packColumn(enTable, i, 2);
                }
            }
        });

        foreignSearchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeAllRows(tableModelForeign);
                String searchQuery = getExactMatchQuery(foreignInput.getText(), SqlVariables.columnTranslation.getColumnName());
                String searchAlikeQuery = getAlikeQuery(foreignInput.getText(), SqlVariables.columnTranslation.getColumnName());

                //log.info("Search Query: " + searchQuery);
                SqlActions sqlActions = new SqlActions(plugin);
                String[][] results = sqlActions.executeSearchQuery(searchQuery);
                String[][] resultsAlike = sqlActions.executeSearchQuery(searchAlikeQuery);
                //log.info("Results: " + results.length);
                for(String[] result : results){
                    tableModelForeign.addRow(new String[]{result[1], result[0], result[2]});
                }
                for(String[] result : resultsAlike){
                    tableModelForeign.addRow(new String[]{result[1], result[0], result[2]});
                }

                // adjust column width
                for (int i = 0; i < tableModelForeign.getColumnCount(); i++) {
                    packColumn(foreignTable, i, 2);
                }
            }
        });

        englishInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                englishSearchButton.doClick();
            }
        });

        foreignInput.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                foreignSearchButton.doClick();
            }
        });
    }

    private void translatePanelTexts(LangCodeSelectableList targetLanguage) {
        if (targetLanguage == LangCodeSelectableList.ENGLISH) {
            textSearchEnglish = "Search with English name";
            textSearchForeign = "Search with Foreign name";
            textSearch = "Search";
            textResult = "Result";
            textType = "Type";
        } /*else if (targetLanguage == LangCodeSelectableList.PORTUGUÊS_BRASILEIRO) {
            textSearchEnglish = "Pesquisar com nome em inglês";
            textSearchForeign = "Pesquisar com nome estrangeiro";
            textSearch = "Pesquisa";
            textResult = "Resultado";
            textType = "Tipo";
        } else if (targetLanguage == LangCodeSelectableList.NORSK) {
            textSearchEnglish = "Søk med engelsk navn";
            textSearchForeign = "Søk med utenlandsk navn";
            textSearch = "Søk";
            textResult = "Resultat";
            textType = "Type";
        } */else if (targetLanguage == LangCodeSelectableList.日本語) {
            textSearchEnglish = "英語名で検索";
            textSearchForeign = "日本語名で検索";
            textSearch = "検索";
            textResult = "結果";
            textType = "タイプ";
        }// todo: add more languages as needed
    }

    private String getExactMatchQuery(String searchText, String searchColumnName) {
        return "SELECT " +
                SqlVariables.columnEnglish.getColumnName() + "," +
                SqlVariables.columnTranslation.getColumnName() + "," +
                SqlVariables.columnSubCategory.getColumnName() +
                " FROM " + SqlActions.tableName +
                " WHERE LOWER(" + searchColumnName + ")" +
                " = '" + searchText.toLowerCase().replace("'","''") + "'" +
                " AND " + SqlVariables.columnCategory.getColumnName() +
                " = '" + SqlVariables.nameInCategory.getValue() +"'";
    }

    private String getAlikeQuery(String searchText, String searchColumnName) {
        return "SELECT " +
                SqlVariables.columnEnglish.getColumnName() + "," +
                SqlVariables.columnTranslation.getColumnName() + "," +
                SqlVariables.columnSubCategory.getColumnName() +
                " FROM " + SqlActions.tableName +
                " WHERE LOWER(" + searchColumnName + ")" +
                " LIKE '%" + searchText.toLowerCase().replace("'","''") + "%'" +
                " AND " + SqlVariables.columnCategory.getColumnName() +
                " = '" + SqlVariables.nameInCategory.getValue() +"'";
    }

    private JTable addEnResultTable(){
        DefaultTableModel tableModelEn = new DefaultTableModel();
        JTable tableEn = new JTable(tableModelEn);
        tableEn.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setTableColumn(tableModelEn);
        JScrollPane pane = new JScrollPane(tableEn, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); // Enable horizontal scrollbar
        pane.setPreferredSize(new Dimension(250, 250));
        this.sidePanel.add(pane);
        return tableEn;
    }

    private JTable addForeignResultTable(){
        DefaultTableModel tableModel = new DefaultTableModel();
        JTable table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setTableColumn(tableModel);
        JScrollPane pane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); // Enable horizontal scrollbar
        pane.setPreferredSize(new Dimension(200, 250));
        this.sidePanel.add(pane);
        return table;
    }

    private void setTableColumn(DefaultTableModel table) {
        table.addColumn(textSearch);
        table.addColumn(textResult);
        table.addColumn(textType);
    }

    private void removeAllRows(DefaultTableModel tableModel) {
        while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
        }
    }

    public void packColumn(JTable table, int vColIndex, int margin) {
        DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
        TableColumn col = colModel.getColumn(vColIndex);
        int width;

        // Get width of column header
        TableCellRenderer renderer = col.getHeaderRenderer();
        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }
        Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
        width = comp.getPreferredSize().width;

        // Get maximum width of column data
        for (int r = 0; r < table.getRowCount(); r++) {
            renderer = table.getCellRenderer(r, vColIndex);
            comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, vColIndex), false, false, r, vColIndex);
            width = Math.max(width, comp.getPreferredSize().width);
        }

        // Add margin
        width += 2 * margin;

        // Set the width
        col.setPreferredWidth(width);
    }

    private JLabel createTextLabel(String text){
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setFont(new Font("MS Gothic", Font.PLAIN, 14));//todo: change the font if a language requires it
        label.setPreferredSize(new Dimension(200, 20));
        label.setForeground(Color.cyan);
        return label;
    }

}
