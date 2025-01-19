package com.RuneLingual.SQL;

import java.sql.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;
import java.util.*;

import com.RuneLingual.commonFunctions.FileNameAndPath;
import com.RuneLingual.prepareResources.Downloader;
import com.RuneLingual.RuneLingualPlugin;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class SqlActions {

    public static final String tableName = "transcript";
    static final String databaseFileName = FileNameAndPath.getLocalSQLFileName();
    ;
    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    private Downloader downloader;
    @Inject
    private FileNameAndPath fileNameAndPath;

    @Inject
    public SqlActions(RuneLingualPlugin plugin) {
        this.plugin = plugin;
    }

    // private String databaseUrl = "jdbc:h2:" + downloader.getLocalLangFolder() + File.separator + databaseFileName;

    public void createTable(String databaseFolder) throws SQLException {
        Connection conn = DriverManager.getConnection(this.plugin.getDatabaseUrl());
        this.plugin.setConn(conn);

        // then create new table
        String sql = "CREATE TABLE " + tableName + " ()";

        try (Statement stmt = plugin.getConn().createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void tsvToSqlDatabase(String[] tsvFiles, String tsvFolderPath){
        // note: table must exist before calling this function

        for (String tsvFile : tsvFiles) {
            processTsvFile(tsvFolderPath + File.separator + tsvFile);
        }

        // index the english column
            String sql = "CREATE INDEX english_index ON " + tableName + " ("+ SqlVariables.columnEnglish.getColumnName()
                    + ", " + SqlVariables.columnTranslation.getColumnName()
                    + ", " + SqlVariables.columnCategory.getColumnName()
                    + "," + SqlVariables.columnSubCategory.getColumnName()
                    + "," + SqlVariables.columnSource.getColumnName() + ")";
            try (Statement stmt = this.plugin.getConn().createStatement()) {
                stmt.execute(sql);
            }catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void processTsvFile(String tsvFilePath) {
        log.info("Processing TSV file: " + tsvFilePath);
        try {
            List<String> lines = Files.readAllLines(Paths.get(tsvFilePath));
            String[] columnNames = lines.get(0).split("\t");

            // Ensure all columns exist
            ensureColumnsExist(columnNames);

            // Insert data
            for (int i = 1; i < lines.size(); i++) {
                if(lines.get(i).split("\t").length > columnNames.length){
                    log.info("Warning processing TSV file " + tsvFilePath + " at line " + i + " : " + lines.get(i));
                    log.info("found more values than number of columns.");
                    log.info("Column names: " + Arrays.toString(columnNames));
                    log.info("Column values: " + Arrays.toString(lines.get(i).split("\t")));
                }
                String[] fields = lines.get(i).split("\t", columnNames.length);

                StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (");

                for (String columnName : columnNames) {
                    sql.append(columnName).append(",");
                }

                sql.deleteCharAt(sql.length() - 1); // remove last comma
                sql.append(") VALUES (");

                for (int j = 0; j < fields.length; j++) {
                    sql.append("?,");
                }

                sql.deleteCharAt(sql.length() - 1); // remove last comma
                sql.append(")");

                try (PreparedStatement pstmt = this.plugin.getConn().prepareStatement(sql.toString())) {
                    for (int j = 0; j < fields.length; j++) {
                        pstmt.setString(j + 1, fields[j]);
                    }

                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    log.info("Error processing TSV file " + tsvFilePath + " at line " + i + " : '" + lines.get(i) + "'");
                    log.info("sql: '" + sql + "'");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            //log.info("Error processing TSV file " + tsvFilePath);
            e.printStackTrace();
        }
    }

    private void ensureColumnsExist(String[] columnNames) {
        for (String columnName : columnNames) {
            String sql = "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + columnName + " VARCHAR(2000)";
            try (Statement stmt = this.plugin.getConn().createStatement()) {
                stmt.execute(sql);
            } catch (SQLException e) {
                log.info("Error adding column " + columnName + " to " + tableName);
                e.printStackTrace();
            }
        }
    }

    public String[][] executeSearchQuery(String query) {
        /*
        * Execute a search query and return the results as a 2D array
        * eg. SELECT * FROM transcript WHERE english = 'hello'
        * returns [["hello", "hola"], ["hello", "こんにちは"]]
         */

        List<List<String>> results = new ArrayList<>();
        try (Statement stmt = this.plugin.getConn().createStatement();){
            ResultSet rs = stmt.executeQuery(query);

            ResultSetMetaData rsmd = rs.getMetaData();
            int columnsNumber = rsmd.getColumnCount();

            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columnsNumber; i++) {
                    row.add(rs.getString(i));
                }
                results.add(row);
            }
            String[][] array = new String[results.size()][];
            for (int i = 0; i < results.size(); i++) {
                List<String> row = results.get(i);
                array[i] = row.toArray(new String[0]);
            }
            return array;
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return new String[0][0];
    }

    public String[] executeQuery(String query) {
        /*
            * Execute a query and return the results as a 1D array
            * eg. SELECT translation FROM transcript WHERE english = 'hello'
            * returns ["hola", "こんにちは"]
         */
        List<String> results = new ArrayList<>();
        try (Statement stmt = this.plugin.getConn().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String columnValue = rs.getString(1);
                results.add(columnValue);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results.toArray(new String[0]);
    }

}
