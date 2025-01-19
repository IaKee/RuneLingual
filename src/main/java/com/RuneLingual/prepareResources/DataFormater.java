package com.RuneLingual.prepareResources;

import com.RuneLingual.commonFunctions.FileActions;
import com.RuneLingual.commonFunctions.FileNameAndPath;
import com.RuneLingual.SQL.SqlActions;
import com.RuneLingual.RuneLingualPlugin;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class DataFormater {

    @Inject
    private RuneLingualPlugin plugin;
    @Inject
    public DataFormater(RuneLingualPlugin plugin){
        this.plugin = plugin;
    }
    @Inject
    private SqlActions sqlActions;

    public void updateSqlFromTsv(String localLangFolder, String[] tsvFileNames){
        log.info("Updating SQL database from TSV files.");
        String SQLFilePath = localLangFolder + File.separator + FileNameAndPath.getLocalSQLFileName() + ".mv.db";
        String SQLFilePath2 = localLangFolder + File.separator + FileNameAndPath.getLocalSQLFileName() + ".trace.db";

        if (FileActions.fileExists(SQLFilePath)){
            FileActions.deleteFile(SQLFilePath);
        }
        if (FileActions.fileExists(SQLFilePath2)){
            FileActions.deleteFile(SQLFilePath2);
        }
        try {
            sqlActions.createTable(localLangFolder);
            log.info("Table created.");
            sqlActions.tsvToSqlDatabase(tsvFileNames, localLangFolder);
        } catch (Exception e) {
            log.error("Error creating table.");
            e.printStackTrace();

        }
    }

}
