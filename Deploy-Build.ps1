remove-item app/build/outputs/apk/fantasyeplmatchtracker.apk

.\gradlew assembleDebug

rename-item app/build/outputs/apk/app-debug.apk fantasyeplmatchtracker.apk

aws s3 sync app\build\outputs\apk s3://fantasyeplmatchtracker/bin/  --acl public-read
#aws s3 sync s3://fantasyeplmatchtracker/bin s3://fantasyeplmatchtracker/bin
Write-Host "https://s3.amazonaws.com/fantasyeplmatchtracker/bin/fantasyeplmatchtracker.apk"