@ECHO OFF
SETLOCAL
SET ERROR_CODE=0
SET WRAPPER_JAR=.mvn\wrapper\maven-wrapper.jar
SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
SET PROPERTIES_FILE=.mvn\wrapper\maven-wrapper.properties
FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%PROPERTIES_FILE%") DO (
  IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
)
IF NOT EXIST "%WRAPPER_JAR%" (
  IF "%WRAPPER_URL%"=="" (
    ECHO wrapperUrl not found in "%PROPERTIES_FILE%"
    EXIT /B 1
  )
  POWERSHELL -NoProfile -Command "Invoke-WebRequest -UseBasicParsing -OutFile \"%WRAPPER_JAR%\" \"%WRAPPER_URL%\""
)
SET JAVA_EXE=java.exe
IF DEFINED JAVA_HOME (
  SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
)
IF NOT EXIST "%JAVA_EXE%" (
  ECHO Java not found. Please install JDK and set JAVA_HOME.
  EXIT /B 1
)
SET MAVEN_PROJECTBASEDIR=%CD%
"%JAVA_EXE%" -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %*
SET ERROR_CODE=%ERRORLEVEL%
ENDLOCAL & EXIT /B %ERROR_CODE%
