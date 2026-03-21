#!/usr/bin/env bash
# Script để chạy Maven với JDK 21 (Oracle JDK)
# Sử dụng: ./mvn.sh <maven-command>
# Ví dụ:
#   ./mvn.sh clean compile
#   ./mvn.sh spring-boot:run
#   ./mvn.sh clean test

export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

echo "Using JDK: $(java -version 2>&1 | head -1)"
mvn "$@"
