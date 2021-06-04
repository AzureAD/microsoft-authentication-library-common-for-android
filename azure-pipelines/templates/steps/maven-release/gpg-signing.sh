#!/bin/bash
sudo apt-get update
sudo apt-get install -y gnupg2

GPG_TTY=$TTY
export GPG_TTY

gpg --import $PUBLIC_SECUREFILEPATH
gpg --import $PRIVATE_SECUREFILEPATH

if [ $JAR == "true" ]; then
    gpg --batch --no-use-agent --passphrase-file $PASSPHRASE_SECUREFILEPATH --armor --detach-sign $PROJECT-$PROJECTVERSION.jar
fi

if [ $AAR == "true" ]; then
    gpg --batch --no-use-agent --passphrase-file $PASSPHRASE_SECUREFILEPATH --armor --detach-sign $PROJECT-$PROJECTVERSION.aar
fi

if [ $SOURCESJAR == "true" ]; then
    gpg --batch --no-use-agent --passphrase-file $PASSPHRASE_SECUREFILEPATH --armor --detach-sign $PROJECT-$PROJECTVERSION-sources.jar
fi

if [ $JAVADOCJAR == "true" ]; then
    gpg --batch --no-use-agent --passphrase-file $PASSPHRASE_SECUREFILEPATH --armor --detach-sign $PROJECT-$PROJECTVERSION-javadoc.jar
fi

gpg --batch --no-use-agent --passphrase-file $PASSPHRASE_SECUREFILEPATH --armor --detach-sign pom-default.xml

for file in *; do
    if [[ -f $file ]]; then
        md5sum -- $file > ${file}.md5
        shasum -- $file > ${file}.sha1
    fi
done
