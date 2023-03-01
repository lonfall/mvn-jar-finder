package com.lh.mvnjarfinder.finder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自动查找输出maven入库语句
 * 使用方法：直接运行，运行之前修改application.yml中的
 * file.search.dir ： 文件查找目录
 * file.maven.repository ： 本地仓库目录
 */
@Component
public class DirFinder {
    private static String path;

    @Value("${file.search.dir}")
    private void setPath(String path) {
        DirFinder.path = path;
    }

    private static String mvn_path;

    @Value("${file.maven.repository}")
    private void setMvn_path(String mvn_path) {
        DirFinder.mvn_path = mvn_path;
    }

    @PostConstruct
    public void init() {
        System.out.println("=====================================开始初始化=====================================");
        findDir(path);
    }

    @PreDestroy
    public void destroy() {
        System.out.println("=====================================程序结束=======================================");
    }


    public void findDir(String path) {
        File file = new File(path);
        findFile(file);
    }

    private void findFile(File file) {
        if (file.isDirectory()) {
            String jarFileName = "", pomFileName = "", jarFilePath = "";
            File pomFile = null;
            for (File f : Objects.requireNonNull(file.listFiles())) {
                if (f.isDirectory()) {
                    findFile(f);
                }
                if (f.getName().endsWith(".jar")) {
                    jarFileName = f.getName();
                    jarFilePath = f.getPath();
                }
                if (f.getName().endsWith(".pom")) {
                    pomFileName = f.getName();
                    pomFile = f;
                }
            }
            // 必须两个同时存在
            if (jarFileName.isEmpty() || pomFileName.isEmpty()) {
                return;
            }
            String groupId = findXmlPath(pomFile, "$project.groupId");
            String artifactId = findXmlPath(pomFile, "$project.artifactId");
            String version = findXmlPath(pomFile, "$project.version");
            if (null != version && version.startsWith("$")) {
                version = findXmlPath(pomFile, "$project." + version.substring(2, version.length() - 1));
            }
//            System.out.println("find maven file is jar:" + jarFileName + " pom:" + pomFileName);
//            System.out.println("in dir: " + file.getPath());
            System.out.println("mvn install:install-file \"-Dmaven.repo.local=" + DirFinder.mvn_path + "\" \"-DgroupId=" + groupId + "\" \"-DartifactId=" + artifactId + "\" \"-Dversion=" + version + "\" \"-Dpackaging=jar\" \"-Dfile=" + jarFilePath + "\" \n");
        } else {
            System.out.println(file.getName() + " 不是目录");
        }
    }

    /**
     * 获取xml路径参数
     * 不保证路径绝对正确，比如说$project.parent.groupId等效于$project.groupId
     * 但反之$project.groupId不等效于$project.parent.groupId
     * 用法示例：findXmlPath(pomFile, "$project.groupId")
     *
     * @param pomFile xml文件
     * @param path    解析路径地址
     * @return xml路径参数
     */
    private String findXmlPath(File pomFile, String path) {
        if (path.charAt(0) != '$') {
            return null;
        }
        // 获取pom文件内容
        StringBuffer pomStr = new StringBuffer();
        try (FileReader fileReader = new FileReader(pomFile)) {
            char[] buffer = new char[1024];
            while (fileReader.read(buffer) > 0) {
                pomStr.append(buffer);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 获取xml路径值
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < path.length(); i++) {
            if (path.charAt(i) != '.') {
                sb.append(path.charAt(i));
            } else {
                Pattern compile = Pattern.compile("<" + sb + "[^>]*>([\\s\\S]*?)</" + sb + ">");
                Matcher matcher = compile.matcher(pomStr);
                if (matcher.find()) {
                    pomStr = new StringBuffer(matcher.group(1));
                } else {
                    return null;
                }
                sb.delete(0, sb.length());
            }
        }
        Pattern compile = Pattern.compile("<" + sb + "[^>]*>([\\s\\S]*?)</" + sb + ">");
        Matcher matcher = compile.matcher(pomStr);
        if (matcher.find()) {
            pomStr = new StringBuffer(matcher.group(1));
        } else {
            return null;
        }
        return pomStr.toString();
    }

}
