@echo off
REM Compila e executa a aplicacao (Windows). Requer JDK 17+.
cd /d "%~dp0"
if exist bin rmdir /s /q bin
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d bin -cp lib\mysql-connector-j-8.2.0.jar @sources.txt || (del sources.txt & pause & exit /b 1)
del sources.txt
java -cp "bin;lib\mysql-connector-j-8.2.0.jar" main.Exec
