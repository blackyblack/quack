@ECHO OFF

for %%X in (java.exe) do (set IS_JAVA_IN_PATH=%%~$PATH:X)

IF defined IS_JAVA_IN_PATH (
	start "NxtPass" java -cp quack.jar;lib\*;conf blackyblack.Application
) ELSE (
	IF EXIST "%PROGRAMFILES%\Java\jre7" (
		start "NxtPass" "%PROGRAMFILES%\Java\jre7\bin\java.exe" -cp quack.jar;lib\*;conf blackyblack.Application
	) ELSE (
		IF EXIST "%PROGRAMFILES(X86)%\Java\jre7" (
			start "NxtPass" "%PROGRAMFILES(X86)%\Java\jre7\bin\java.exe" -cp quack.jar;lib\*;conf blackyblack.Application
		) ELSE (
			ECHO Java software not found on your system. Please go to http://java.com/en/ to download a copy of Java.
			PAUSE
		)
	)
)
