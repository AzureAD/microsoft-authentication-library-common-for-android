#!/bin/bash
sudo apt-get update
sudo apt-get install -y gnupg2

GPG_TTY=$TTY
export GPG_TTY

GPG_DIR=~/.gnupg

mkdir -p $GPG_DIR
chmod 700 $GPG_DIR
GPG_FILE="$GPG_DIR/gpg.conf"
touch $GPG_FILE
echo 'use-agent' > $GPG_FILE
echo 'pinentry-mode loopback' >> $GPG_FILE
echo 'batch' >> $GPG_FILE
echo 'no-tty' >> $GPG_FILE

GPG_AGENT_FILE="$GPG_DIR/gpg-agent.conf"
echo 'allow-loopback-pinentry' > $GPG_AGENT_FILE
echo 'RELOADAGENT' | gpg-connect-agent

gpg --import $PUBLIC_SECUREFILEPATH
gpg --import $PRIVATE_SECUREFILEPATH

signed=0

if [ $JAR == "true" ]; then
    gpg --batch --pinentry-mode loopback --passphrase-file $PASSPHRASE_SECUREFILEPATH --armor --detach-sign $PROJECT-$PROJECTVERSION.jar
    signed=$?
    if [ $signed -ne 0 ]; then
        echo "GPG signing failed with for $PROJECT-$PROJECTVERSION.jar with error code $signed"
        exit $signed
    fi
    if [ ! -f $PROJECT-$PROJECTVERSION.jar.asc ]; then
        exit "Signature file for $PROJECT-$PROJECTVERSION.jar not found."
    fi
fi

if [ $AAR == "true" ]; then
    gpg --batch --pinentry-mode loopback --passphrase-file $PASSPHRASE_SECUREFILEPATH --armor --detach-sign $PROJECT-$PROJECTVERSION.aar
    signed=$?
    if [ $signed -ne 0 ]; then
        echo "GPG signing failed with for $PROJECT-$PROJECTVERSION.aar with error code $signed"
        exit $signed
    fi
    if [ ! -f $PROJECT-$PROJECTVERSION.aar.asc ]; then
        exit "Signature file for $PROJECT-$PROJECTVERSION.aar not found."
    fi
fi

if [ $SOURCESJAR == "true" ]; then
    gpg --batch --pinentry-mode loopback --passphrase-file $PASSPHRASE_SECUREFILEPATH --armor --detach-sign $PROJECT-$PROJECTVERSION-sources.jar
    signed=$?
    if [ $signed -ne 0 ]; then
        echo "GPG signing failed with for $PROJECT-$PROJECTVERSION-sources.jar with error code $signed"
        exit $signed
    fi
    if [ ! -f $PROJECT-$PROJECTVERSION-sources.jar.asc ]; then
        exit "Signature file for $PROJECT-$PROJECTVERSION-sources.jar not found."
    fi
fi

if [ $JAVADOCJAR == "true" ]; then
    gpg --batch --pinentry-mode loopback --passphrase-file $PASSPHRASE_SECUREFILEPATH --armor --detach-sign $PROJECT-$PROJECTVERSION-javadoc.jar
    signed=$?
    if [ $signed -ne 0 ]; then
        echo "GPG signing failed with for $PROJECT-$PROJECTVERSION-javadoc.jar with error code $signed"
        exit $signed
    fi
    if [ ! -f $PROJECT-$PROJECTVERSION-javadoc.jar.asc ]; then
        exit "Signature file for $PROJECT-$PROJECTVERSION-javadoc.jar not found."
    fi
fi

if [ $TESTFIXTUREJAR == "true" ]; then
    gpg --batch --pinentry-mode loopback --passphrase-file $PASSPHRASE_SECUREFILEPATH --armor --detach-sign $PROJECT-$PROJECTVERSION-test-fixtures.jar
    signed=$?
    if [ $signed -ne 0 ]; then
        echo "GPG signing failed with for $PROJECT-$PROJECTVERSION-test-fixtures.jar with error code $signed"
        exit $signed
    fi
    if [ ! -f $PROJECT-$PROJECTVERSION-javadoc.jar.asc ]; then
        exit "Signature file for $PROJECT-$PROJECTVERSION-test-fixtures.jar not found."
    fi
fi

gpg --batch --pinentry-mode loopback --passphrase-file $PASSPHRASE_SECUREFILEPATH --armor --detach-sign pom-default.xml
signed=$?

if [ $signed -ne 0 ]; then
    echo "GPG signing failed with for pom-default.xml with error code $signed"
    exit $signed
fi
if [ ! -f pom-default.xml.asc ]; then
    exit "Signature file for pom-default.xml not found."
fi

for file in *; do
    if [[ -f $file ]]; then
        md5sum -- $file > ${file}.md5
        shasum -- $file > ${file}.sha1
    fi
done
