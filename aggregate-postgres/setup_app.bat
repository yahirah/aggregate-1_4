D:
cd "D:\Libraries\Tomcat 6\"
cd webapps\
del /Q /S "aggregate-postgres-1.0.1"
rd /Q /S "aggregate-postgres-1.0.1"
copy "E:\Projekty\Studia\aggregate-1_4\aggregate-postgres\target\aggregate-postgres-1.0.1.war" "D:\Libraries\Tomcat 6\webapps\aggregate-postgres-1.0.1.war"
cd ..\bin\
start startup.bat