package oracle.paas.accs.deployer.spi.accs.util;

import org.apache.commons.io.FileUtils;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ACCSUtil {

    public static final String APP_RUNNER = "appRunner.sh";
    public static final String MANIFEST_FILE = "manifest.json";
    public static final String DEPLOYMENT_FILE = "deployment.json";

    private static Logger logger = Logger.getLogger(ACCSUtil.class.getName());

    public static String getSanitizedApplicationName(String name) {
       return name.replaceAll("[^A-Za-z0-9]", "");
    }

    public static File convertToZipFile(File file, String command) {
        if(file.exists()) {

            String zipName = file.getName().replace(".jar", ".zip");
            try {

                File runnerFile = new File(APP_RUNNER);
                FileUtils.writeStringToFile(runnerFile, command);

                List<File> filesToZip = new ArrayList<File>();
                filesToZip.add(file);
                filesToZip.add(runnerFile);
                zipFiles(filesToZip, zipName);
            } catch(Exception e) {
                logger.log(Level.SEVERE, "Unable to create zip :", e);
                throw new RuntimeException(e);
            }
            return new File(zipName);
        }
        return null;
    }

    private static void zipFiles(List<File> files, String zipName) throws Exception{
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipName));
            for (File file : files) {
                FileInputStream fis = new FileInputStream(file);
                zos.putNextEntry(new ZipEntry(file.getName()));

                byte[] byteBuffer = new byte[1024];
                int bytesRead = -1;
                while ((bytesRead = fis.read(byteBuffer)) != -1) {
                    zos.write(byteBuffer, 0, bytesRead);
                }

                zos.closeEntry();
                fis.close();
            }
        }finally {
            try {
                if(zos != null) {
                    zos.finish();
                    zos.close();
                }
            } catch (Exception e) {
            }
        }
    }
    
    public static void deleteFile (File file) {
        try {
            if (file != null && file.exists()) {
                file.delete();
            }
        } catch (SecurityException se){
            logger.log(Level.SEVERE, "Failed to delete file : " + file.getAbsolutePath(), se);
        }
    }
    
    public static void deleteCommonFiles () {
        try {
            deleteFile(new File(ACCSUtil.APP_RUNNER));
            deleteFile(new File(ACCSUtil.MANIFEST_FILE));
            deleteFile(new File(ACCSUtil.DEPLOYMENT_FILE));
        } catch (Exception se){
            logger.log(Level.SEVERE, "Exception while deleting comon files ", se);
        }
    }
}

