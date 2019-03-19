package pressFiles;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 压缩文件夹下的所有文件 并删除文件和空文件
 */
public final class App {
    /**
     * 补充文件名称
     * 
     * @param fileName 文件名
     */
    private static String supplementFileName(String fileName) {
        String resultName = fileName;
        String[] originName = resultName.split("-");
        boolean changeFlag = false;
        if (originName.length == 3) {
            if (originName[1].length() == 1) {
                originName[1] = "0" + originName[1];
                changeFlag = true;
            }
            originName[1] = "-" + originName[1];

            if (originName[2].length() == 5) {
                originName[2] = "0" + originName[2];
                changeFlag = true;
            }
            originName[2] = "-" + originName[2];

            if (changeFlag) {
                StringBuffer root = new StringBuffer();
                for (String s : originName) {
                    root = root.append(s);
                }
                resultName = root.toString();
            }
        }

        return resultName;
    }

    /**
     * 给文件夹排序
     * 
     * @param sourceSet 被排序的数组
     */
    private static List<File> orderByName(File[] sourceSet) {
        File[] resultSet = sourceSet;
        List<File> fileList = Arrays.asList(resultSet);
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isFile())
                    return -1;
                if (o1.isFile() && o2.isDirectory())
                    return 1;

                String o1Name = o1.getName();
                String o2Name = o2.getName();
                o1Name = supplementFileName(o1Name);
                o2Name = supplementFileName(o2Name);

                return o2Name.compareTo(o1Name);
            }
        });
        return fileList;
    }

    /**
     * 压缩多个文件成一个zip文件
     * 
     * @param srcfile 源文件列表
     * @param zipfile 压缩后的文件
     */
    public static void zipFiles(File[] srcfile, File zipfile) {
        byte[] buf = new byte[1024];
        try {
            // ZipOutputStream类：完成文件或文件夹的压缩
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
            for (int i = 0; i < srcfile.length; i++) {
                FileInputStream in = new FileInputStream(srcfile[i]);
                out.putNextEntry(new ZipEntry(srcfile[i].getName()));
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();
            System.out.println("压缩完成.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 遍历目录，压缩，删除
     * 
     * @param inputPath    源数据路径
     * @param outputPath   目的数据路径
     * @param pressFileNum 压缩文件阈值
     */
    private static int[] traverseFolder(String inputPath, String outputPath, int pressFileNum) {
        int fileNum = 0, folderNum = 0;
        File inputFolder = new File(inputPath);
        Stack<File> visitStack = new Stack<>(); // 遍历用堆栈

        if (inputFolder.exists()) {
            // 路径存在
            LinkedList<File> fileList = new LinkedList<>(); // 被压缩的文件列表
            List<File> fileSet = orderByName(inputFolder.listFiles());
            File tempFile;

            if (fileSet != null) {
                // 深度优先遍历
                for (int index = 0; index < fileSet.size(); ++index) {
                    visitStack.push(fileSet.get(index));
                }

                while (!visitStack.empty()) {
                    tempFile = visitStack.pop();

                    if (tempFile.isDirectory()) {
                        // 文件夹
                        List<File> tempChildren = orderByName(tempFile.listFiles());
                        for (int index = 0; index < tempChildren.size(); ++index) {
                            visitStack.push((File) tempChildren.get(index));
                        }
                        ++folderNum;
                    } else {
                        // 文件
                        fileList.add(tempFile);
                        ++fileNum;
                    }

                    if (fileList.size() >= pressFileNum) {
                        // 开始压缩
                        String firstName = fileList.get(0).getName();
                        firstName = firstName.substring(4, firstName.length() - 4);
                        String lastName = fileList.get(fileList.size() - 1).getName();
                        lastName = lastName.substring(4, lastName.length() - 4);
                        String folderName = outputPath + "/" + firstName + "_" + lastName + ".zip";
                        File outputFile = new File(folderName);
                        if (!outputFile.exists()) {
                            try {
                                outputFile.createNewFile();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        System.out.println(folderName + "\n");

                        zipFiles((File[]) fileList.toArray(new File[fileList.size()]), outputFile);

                        // 清除数据
                        // 1. 删除压缩文件
                        Queue<File> pressFolderList = new LinkedList<>(); // 父目录
                        for (int index = 0; index < fileList.size(); ++index) {
                            File deletedFile = fileList.get(index);
                            pressFolderList.offer(deletedFile.getParentFile());
                            deletedFile.delete();
                        }
                        fileList.clear();
                        System.out.print("文件清除" + "\n");

                        // 2. 检查文件夹是否为空
                        // 空则递归删除
                        while (!pressFolderList.isEmpty()) {
                            File manageFolderFile = pressFolderList.poll();
                            if (manageFolderFile.exists() && manageFolderFile != inputFolder) {
                                File[] childrenFileList = manageFolderFile.listFiles();
                                if (childrenFileList.length == 0) {
                                    pressFolderList.offer(manageFolderFile.getParentFile());
                                    manageFolderFile.delete();
                                }
                            }
                        }
                        System.out.println("文件夹回收完成" + "\n");
                    }
                }
            }
        }

        int[] result = { fileNum, folderNum };
        return result;
    }

    /**
     * 压缩文件
     * 
     * @param args The arguments of the program.
     */
    public static void main(String[] args) {
        String propertiesPath = "./config.properties";
        String inputPath = "";
        String outputPath = "";
        int pressFileNum = 0;

        try {
            InputStream is = new BufferedInputStream(new FileInputStream(new File(propertiesPath)));
            Properties properties = new Properties();
            properties.load(is);
            is.close();
            inputPath = properties.getProperty("inputPath");
            outputPath = properties.getProperty("outputPath");
            pressFileNum = Integer.parseInt(properties.getProperty("pressFileNum"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        int[] totalFilesNumber = traverseFolder(inputPath, outputPath, pressFileNum);

        System.out.println(
                "manage file num: " + totalFilesNumber[0] + "\n" + "manage folder num: " + totalFilesNumber[1] + "\n");
        System.out.println("finish\n");
    }
}
