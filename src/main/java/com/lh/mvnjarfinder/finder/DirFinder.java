package com.lh.mvnjarfinder.finder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

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
            if (null == groupId) {
                groupId = findXmlPath(pomFile, "$project.parent.groupId");
            }
            String artifactId = findXmlPath(pomFile, "$project.artifactId");
            String version = findXmlPath(pomFile, "$project.version");
            if (null == version) {
                version = findXmlPath(pomFile, "$project.parent.version");
            }
            if (null != version && version.startsWith("$")) {
                version = findXmlPath(pomFile, "$project." + version.substring(2, version.length() - 1));
            }
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
            int data;
            while ((data = fileReader.read()) != -1) {
                pomStr.append((char) data);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 获取xml路径值
        StringBuffer sb = new StringBuffer();
        for (int i = 1; i < path.length(); i++) {
            if (path.charAt(i) != '.') {
                sb.append(path.charAt(i));
            } else {
                StringBuffer between = getBetween(pomStr, sb.toString());
                if (between == null) {
                    return null;
                }
                pomStr = between;
                sb.delete(0, sb.length());
            }
        }
        StringBuffer between = getBetween(pomStr, sb.toString());
        if (between == null) {
            return null;
        }
        pomStr = between;
        return pomStr.toString();
    }

    /**
     * 获取xml对应name中间的值
     * 只获取最外层的
     * 列如<a><b>1</b><a/><b>2</b>中name为b
     * 只会判断<a><b>1</b></a>和<b>2</b>
     * 最后获取的值为2
     *
     * @param pomStr
     * @param name
     * @return
     */
    private StringBuffer getBetween(StringBuffer pomStr, String name) {
        if (null == pomStr) {
            return null;
        }
        int index = 0;
        int left_count = 0;
        int right_count = 0;
        while (index < pomStr.length()) {
            if ("<".equals("" + pomStr.charAt(index))) {
                StringBuffer open_name = new StringBuffer();
                StringBuffer close_name = new StringBuffer();
                StringBuffer between = new StringBuffer();
                index++;
                int l = 0, r = 0;
                while (index < pomStr.length()) {
                    if ("<".equals("" + pomStr.charAt(index))) {
                        l++;
                    }
                    if (">".equals("" + pomStr.charAt(index))) {
                        if (r == l) {
                            index++;
                            break;
                        }
                        r++;
                    }
                    open_name.append(pomStr.charAt(index));
                    index++;
                }
                if (open_name.toString().startsWith("?")) {
                } else if (open_name.toString().startsWith("!")) {
                } else if (open_name.toString().endsWith("/")) {
                } else if (open_name.toString().startsWith("/")) {
                    right_count++;
                } else {
                    left_count++;
                }
                if (right_count == (left_count - 1) && open_name.toString().startsWith(name)) {
                    while (index < pomStr.length()) {
                        if ("<".equals("" + pomStr.charAt(index))) {
                            index++;
                            l = 0;
                            r = 0;
                            while (index < pomStr.length()) {
                                if ("<".equals("" + pomStr.charAt(index))) {
                                    l++;
                                }
                                if (">".equals("" + pomStr.charAt(index))) {
                                    if (r == l) {
                                        break;
                                    }
                                    r++;
                                }
                                close_name.append(pomStr.charAt(index));
                                index++;
                            }
                            if (close_name.toString().startsWith("?")) {
                            } else if (close_name.toString().startsWith("!")) {
                            } else if (close_name.toString().endsWith("/")) {
                            } else if (close_name.toString().startsWith("/")) {
                                right_count++;
                            } else {
                                left_count++;
                            }
                            if (right_count == left_count && close_name.toString().startsWith("/" + name)) {
                                return between;
                            } else {
                                between.append("<" + close_name);
                                close_name.delete(0, close_name.length());
                            }
                        }
                        between.append(pomStr.charAt(index));
                        index++;
                    }
                }
                open_name.delete(0, open_name.length());
            }
            index++;
        }
        return null;
    }
}
