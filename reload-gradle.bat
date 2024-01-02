@echo off

:LOOP
echo "Do you want to setup Workspace?"
set /p YN=(Y/N)?

if /i "%YN%" == "y" goto YES
if /i "%YN%" == "n" goto NO

goto LOOP

:YES
echo "Removing files..."
rmdir /S /Q ".gradle/"
rmdir /S /Q ".idea/"
rmdir /S /Q "build/tmp/"
rmdir /S /Q "build/java/"
del /Q "*.iml"
del /Q "*.ipr"
del /Q "*.iws"
del /Q "*.log"

echo "Removed files"

echo "Setup workspaces..."
gradlew build
pause
goto QUIT


:NO
echo "You did not select YES, so no action was taken"
pause
goto QUIT

:QUIT