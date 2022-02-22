#指定jdk版本
FROM java:openjdk-8-jre-alpine
#工作目录
WORKDIR /workspace
#打包完成后target目录中的jar包拷贝到工作目录
COPY target/*.jar /home
#执行对应的java脚本
ENTRYPOINT java -jar *.jar
