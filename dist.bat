@echo off
if exist public\dist (
del /S /F /Q public\dist 
)

cd tight_front
call npm run build
cd ../public
mkdir dist
cd dist
xcopy /E /I ..\..\tight_front\dist
cd ../..
call sbt clean;dist
@echo on
