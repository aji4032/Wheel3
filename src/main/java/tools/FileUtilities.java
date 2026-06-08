package tools;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.Loader;
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
    private static final Logger log = Log.getLogger(FileUtilities.class);

    public static void moveFileUsingThread(File sourceFile, File destinationFile) {
        new FileMover(sourceFile, destinationFile).start();
    }

    public static void deleteFile(File file) {
        boolean status = file.delete();
        if (!status)
            log.fail("Failed to delete file - " + file.getName());
        else
            log.info("Successfully deleted file - " + file.getName());
    }

    public static void deleteDirectory(File directory) {
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException e) {
            log.warn("Failed to delete directory: " + e.getMessage());
        }
    }

    private static ArrayList<String> fileList;
    private static File sourceFolder;

    private static void generateFileList(File node) {
        if (node.isFile()) {
            fileList.add(generateZipEntry(node.toString()));
        }

        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String filename : subNote) {
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
        if (output.exists()) {
            deleteFile(output);
        }
        generateFileList(directoryToBeZipped);

        byte[] buffer = new byte[1024];
        String source = directoryToBeZipped.getName();
        FileOutputStream objFileOutputStream;
        ZipOutputStream objZipOutputStream = null;
        try {
            objFileOutputStream = new FileOutputStream(output.getAbsoluteFile());
            objZipOutputStream = new ZipOutputStream(objFileOutputStream);

            log.info("Output to zip: " + output.getAbsolutePath());
            FileInputStream objFileInputStream = null;

            for (String file : fileList) {
                log.info("File added: " + file);
                ZipEntry objZipEntry = new ZipEntry(source + File.separator + file);
                objZipOutputStream.putNextEntry(objZipEntry);
                try {
                    objFileInputStream = new FileInputStream(
                            directoryToBeZipped.getAbsoluteFile() + File.separator + file);
                    int length;
                    while ((length = objFileInputStream.read(buffer)) > 0) {
                        objZipOutputStream.write(buffer, 0, length);
                    }
                } finally {
                    if (objFileInputStream != null) {
                        objFileInputStream.close();
                    }
                }
            }
            objZipOutputStream.closeEntry();
            log.info("Folder successfully compressed.");
        } catch (IOException ex) {
            log.warn("Failed to zip file: " + ex.getMessage());
        } finally {
            try {
                objZipOutputStream.close();
            } catch (IOException e) {
                log.warn("Failed to zip file: " + e.getMessage());
            }
        }
    }

    public static List<File> extractZipFile(File zipFile, File destinationDirectory) {
        List<File> files = new ArrayList<>();
        try {
            FileInputStream objFileInputStream = new FileInputStream(zipFile);
            ZipInputStream objZipInputStream = new ZipInputStream(objFileInputStream);
            java.nio.file.Path destinationPath = destinationDirectory.toPath().toAbsolutePath().normalize();
            ZipEntry objZipEntry;

            while ((objZipEntry = objZipInputStream.getNextEntry()) != null) {
                String entryFilename = objZipEntry.getName();
                File entryFile = new File(destinationDirectory, entryFilename);
                java.nio.file.Path entryPath = entryFile.toPath().toAbsolutePath().normalize();

                if (!entryPath.startsWith(destinationPath)) {
                    log.warn("Skipping zip entry outside destination directory: " + entryFilename);
                    objZipInputStream.closeEntry();
                    continue;
                }

                if (objZipEntry.isDirectory()) {
                    FileUtilities.createDirectory(entryFile);
                } else {
                    files.add(entryFile);
                    FileUtilities.createDirectory(new File(entryFile.getParent()));
                    FileOutputStream objFileOutputStream = new FileOutputStream(entryFile);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = objZipInputStream.read(buffer)) > 0) {
                        objFileOutputStream.write(buffer, 0, length);
                    }
                    objFileOutputStream.close();
                }
                objZipInputStream.closeEntry();
            }
            objZipInputStream.close();
            objFileInputStream.close();
            log.info(zipFile.getName() + " file unzipped!");
        } catch (IOException e) {
            log.fail("Failed to unzip: " + zipFile.getName());
        }
        return files;
    }

    public static void createDirectory(File directory) {
        if (!directory.mkdirs())
            log.warn("Failed to create directory! - " + directory.getName());
    }

    public static String getTextFromPdf(File pdfFile) {
        try {
            PDDocument objPDDocument = Loader.loadPDF(pdfFile);
            PDFTextStripper objPDFTextStripper = new PDFTextStripper();
            return objPDFTextStripper.getText(objPDDocument);
        } catch (IOException e) {
            log.fail("Failed to get text from pdf! - " + e.getMessage());
            return null;
        }
    }

    public static void saveByte64ToFile(String byte64Data, File file) {
        if (file.exists())
            deleteFile(file);

        byte[] data = Base64.decodeBase64(byte64Data);
        try (FileOutputStream objFileOutputStream = new FileOutputStream(file)) {
            objFileOutputStream.write(data);
        } catch (IOException e) {
            log.fail("Failed to write Byte64 data to file!");
        }
    }

    public static String encodeFileToBase64Binary(File file) {
        String encodedFile = null;
        try (FileInputStream objFileInputStream = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            objFileInputStream.read(bytes);
            encodedFile = new String(Base64.encodeBase64(bytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.fail("Failed to encode - " + e.getMessage());
        }
        return encodedFile;
    }
}

class FileMover extends Thread {
    private static final Logger log = Log.getLogger(FileMover.class);
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
            log.fail("Failed to move file - " + e.getMessage());
        }
    }
}
