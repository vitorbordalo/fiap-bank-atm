#!/usr/bin/env bash
set -e
echo "===================================================="
echo "       FIAP BANK - EMULADOR DE CAIXA ELETRONICO"
echo "===================================================="
mvn clean install
mvn -pl infrastructure exec:java
