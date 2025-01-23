package com.RuneLingual.filemanagement;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.RuneLingual.LangCodeSelectableList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileActions
{

    static String fileNameStart = "setLang_";
    static String langNameFolder = FileNameAndPath
        .getLocalBaseFolder()
        .toString();

    public static LangCodeSelectableList getLangCodeFromFile()
    {
        String existingFileName = getFileNameInFolderStartsWith(
            langNameFolder,
            fileNameStart);
        if(existingFileName != null)
        {
            String langCode = existingFileName
                .substring(
                    existingFileName.indexOf("_") + 1,
                    existingFileName.indexOf("."));

            for(LangCodeSelectableList lang : LangCodeSelectableList.values())
            {
                if (lang.getLangCode().equals(langCode))
                {
                    return lang;
                }
            }

            log.warn("Could not find language code for file '{}'", existingFileName);
            return LangCodeSelectableList.ENGLISH;
        }

        log.warn("Could not find language code. Existing file name is null.");
        return LangCodeSelectableList.ENGLISH;
    }

    public static void createLangCodeNamedFile(LangCodeSelectableList lang)
    {
        String fileName = langNameFolder
            + File.separator
            + fileNameStart
            + lang.getLangCode()
            + ".txt";
        createFile(fileName);
    }

    public static void deleteAllLangCodeNamedFile()
    {
        for (LangCodeSelectableList lang : LangCodeSelectableList.values())
        {
            String fileName = langNameFolder
                + File.separator
                + fileNameStart
                + lang.getLangCode()
                + ".txt";
            deleteFile(fileName);
        }
    }

    public static void createFile(String fileName)
    {
        try
        {
            File myObj = new File(fileName);
            if(myObj.createNewFile())
            {
                log.info("File created: {}", myObj.getName());
            }
            else
            {
                log.error("File '{}' already exists.", fileName);
            }
        }
        catch(IOException e)
        {
            log.error("An error occurred while creating '{}' file:", fileName);
            log.error(e.getMessage());
        }
    }

    public static String getFileNameInFolderStartsWith(String path, String fileName)
    {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if(listOfFiles == null)
        {
            log.error("Returned list of files from path '{}' is null.", path);
            return null;
        }

        for(File file : listOfFiles)
        {
            if (file.isFile())
            {
                if (file.getName().startsWith(fileName))
                {
                    return file.getName();
                }
            }
        }
        return null;
    }

    public static List<String> getFileNamesWithExtension(String path, String extension)
    {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if(listOfFiles == null)
        {
            log.error("Returned list of files from path '{}' is null.", path);
            return null;
        }

        List<String> matchedFileNames = new ArrayList<>();
        for(File file : listOfFiles)
        {
            if(file.isFile())
            {
                if (file.getName().endsWith(extension))
                {
                    matchedFileNames.add(file.getName());
                }
            }
        }
        return matchedFileNames;
    }

    public static boolean deleteFile(String fileName)
    {
        File myObj = new File(fileName);
        if(myObj.delete())
        {
            log.info("Deleted file '{}' successfully!", myObj.getName());
            return true;
        }
        else
        {
            log.error("Could not delete file '{}'!", myObj.getName());
            return false;
        }
    }

    public static boolean fileExists(String filename)
    {
        File myObj = new File(filename);
        return myObj.exists();
    }

    public static void deleteFolder(String folderPath)
    {
        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();
        if(listOfFiles != null)
        {
            for (File file : listOfFiles)
            {
                if(!deleteFile(file.getPath()))
                {
                    log.error("Could not complete folder deletion. Could not delete file '{}' from folder!", file.getName());
                    return;
                }
            }
        }

        if(!folder.delete())
        {
            log.error("Could not complete folder deletion!");
        }
    }
}
