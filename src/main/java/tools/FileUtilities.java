package tools;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtilities {
    public static void moveFileUsingThread(File sourceFile, File destinationFile) {
        new FileMover(sourceFile, destinationFile).start();
    }

    public static void deleteFile(File file) {
        boolean status = file.delete();
        if(!status)
            Log.fail("Failed to delete file - " + file.getName());
        else
            Log.info("Successfully deleted file - " + file.getName());
    }

    public static void deleteDirectory(File directory) {
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            Log.warn("Failed to delete directory: " + e.getMessage());
        }
    }

    private static ArrayList<String> fileList;
    private static File sourceFolder;
    private static void generateFileList(File node) {
        if(node.isFile()) {
            fileList.add(generateZipEntry(node.toString()));
        }

        if(node.isDirectory()) {
            String[] subNote = node.list();
            for(String filename: subNote) {
                generateFileList(new File(node, filename));
            }
        }
    }

    private static String generateZipEntry(String file) {
        return file.substring(sourceFolder.getPath().length() + 1);
    }

    public static void zipDirectory(File directoryToBeZipped, File output) {
        fileList = new ArrayList<String>();
        sourceFolder = directoryToBeZipped;
        if(output.exists()) {
            deleteFile(output);
        }
        generateFileList(directoryToBeZipped);

        byte[] buffer = new byte[1024];
        String source = directoryToBeZipped.getName();
        FileOutputStream objFileOutputStream;
        ZipOutputStream objZipOutputStream = null;
        try {
            objFileOutputStream = new FileOutputStream(output.getAbsoluteFile());
            objZipOutputStream  = new ZipOutputStream(objFileOutputStream);

            Log.info("Output to zip: " + output.getAbsolutePath());
            FileInputStream objFileInputStream = null;

            for(String file: fileList) {
                Log.info("File added: " + file);
                ZipEntry objZipEntry = new ZipEntry(source + File.separator + file);
                objZipOutputStream.putNextEntry(objZipEntry);
                try {
                    objFileInputStream = new FileInputStream(directoryToBeZipped.getAbsoluteFile() + File.separator + file);
                    int length;
                    while((length = objFileInputStream.read(buffer)) > 0) {
                        objFileOutputStream.write(buffer, 0, length);
                    }
                } finally {
                    if (objFileInputStream != null) {
                        objFileInputStream.close();
                    }
                }
            }
            objZipOutputStream.closeEntry();
            Log.info("Folder successfully compressed.");
        } catch (IOException ex) {
            Log.warn("Failed to zip file: " + ex.getMessage());
        } finally {
            try {
                objZipOutputStream.close();
            } catch (IOException e) {
                Log.warn("Failed to zip file: " + e.getMessage());
            }
        }
    }

    public static List<File> extractZipFile(File zipFile, File destinationDirectory) {
        List<File> files = new ArrayList<>();
        try {
            FileInputStream objFileInputStream = new FileInputStream(zipFile);
            ZipInputStream objZipInputStream = new ZipInputStream(objFileInputStream);
            ZipEntry objZipEntry;

            while((objZipEntry = objZipInputStream.getNextEntry()) != null) {
                String entryFilename = objZipEntry.getName();
                File entryFile = new File(destinationDirectory, entryFilename);

                if(objZipEntry.isDirectory()) {
                    FileUtilities.createDirectory(entryFile);
                } else {
                    files.add(entryFile);
                    FileUtilities.createDirectory(new File(entryFile.getParent()));
                    FileOutputStream objFileOutputStream = new FileOutputStream(entryFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while((length = objFileInputStream.read(buffer)) > 0) {
                        objFileOutputStream.write(buffer, 0, length);
                    }
                    objFileOutputStream.close();
                }
                objZipInputStream.closeEntry();
            }
            objZipInputStream.close();
            objFileInputStream.close();
            Log.info(zipFile.getName() + " file unzipped!");
        } catch (IOException e) {
            Log.fail("Failed to unzip: " + zipFile.getName());
        }
        return files;
    }

    public static void createDirectory(File directory) {
        if(!directory.mkdirs())
            Log.warn("Failed to create directory! - " + directory.getName());
    }

    public static String getTextFromPdf(File pdfFile) {
        try {
            PDDocument objPDDocument = PDDocument.load(pdfFile);
            PDFTextStripper objPDFTextStripper = new PDFTextStripper();
            return objPDFTextStripper.getText(objPDDocument);
        } catch(IOException e) {
            Log.fail("Failed to get text from pdf! - " + e.getMessage());
            return null;
        }
    }

    public static void saveByte64ToFile(String byte64Data, File file) {
        if(file.exists())
            deleteFile(file);

        byte[] data = Base64.decodeBase64(byte64Data);
        try(FileOutputStream objFileOutputStream = new FileOutputStream(file)) {
            objFileOutputStream.write(data);
        } catch (IOException e) {
            Log.fail("Failed to write Byte64 data to file!");
        }
    }

    public static String encodeFileToBase64Binary(File file) {
        String encodedFile = null;
        try (FileInputStream objFileInputStream = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            objFileInputStream.read(bytes);
            encodedFile = new String(Base64.encodeBase64(bytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.fail("Failed to encode - " + e.getMessage());
        }
        return encodedFile;
    }
}

class FileMover extends Thread {
    private final File sourceFile;
    private final File destinationFile;
    public FileMover(File sourceFile, File destinationFile) {
        this.sourceFile = sourceFile;
        this.destinationFile = destinationFile;
    }

    public void run() {
        try {
            FileUtils.moveFile(sourceFile, destinationFile);
        } catch (IOException e) {
            Log.fail("Failed to move file - " + e.getMessage());
        }
    }
}
