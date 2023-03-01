# 项目简介
&emsp;&emsp;我们有时需要在maven本地仓库中转移文件，比如从同事的.m2文件夹中复制的某个com.xxx.xxx
文件包，这时直接复制到我们的.m2文件中往往无法正常运行，需要使用maven -install:install-file命令来手动安装，
这个工具包就是用于自动成maven -install:install-file语句的。

使用方法：

    首先配置applicatio.yml中的
    以下配置项

    file.search.dir ： 文件查找目录
    file.maven.repository ： 本地仓库目录

    示例：

    file:
        search:
            dir: D:/Documents/.m2/手动安装目录/ #仓库文件目录
        maven:
            repository: D:\Documents\.m2\repository #本地仓库地址

    之后直接启动项目即可

复制的命令可以直接在命令窗口中复制运行

