#!/bin/sh
# Compila e executa a aplicacao (macOS / Linux). Requer JDK 17+.
cd "$(dirname "$0")"
rm -rf bin
javac -encoding UTF-8 -d bin -cp lib/mysql-connector-j-8.2.0.jar $(find src -name "*.java") || exit 1
java -cp "bin:lib/mysql-connector-j-8.2.0.jar" main.Exec
