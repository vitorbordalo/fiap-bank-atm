@echo off
chcp 65001 > nul
echo ====================================================
echo        FIAP BANK - EMULADOR DE CAIXA ELETRONICO
echo ====================================================
echo.
echo Procurando o Maven do Apache NetBeans...

set MVN_PATH="C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"

if exist %MVN_PATH% (
    echo Maven encontrado! Iniciando a aplicacao...
    call %MVN_PATH% clean install
    call %MVN_PATH% -pl infrastructure exec:java
) else (
    echo.
    echo [AVISO] Maven do NetBeans nao encontrado no caminho padrao.
    echo Tentando usar comando 'mvn' global...
    where mvn >nul 2>nul
    if %errorlevel% equ 0 (
        call mvn clean install
        call mvn -pl infrastructure exec:java
    ) else (
        echo [ERRO] Maven nao encontrado. Por favor, abra este projeto
        echo no Apache NetBeans e execute-o diretamente pelo editor,
        echo ou instale o Maven e adicione-o ao seu PATH.
        pause
    )
)
