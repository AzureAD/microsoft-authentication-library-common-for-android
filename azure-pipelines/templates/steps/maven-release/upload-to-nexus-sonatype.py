import requests
from xml.dom import minidom
import sys
import os
import json

CONFIG = {
    'adal': {'profile_id': '4ac616f169d24a', 'path': 'aad/adal'},
    'msal': {'profile_id': '60bca90c1b7703', 'path': 'identity/client/msal'},
    'common': {'profile_id': '60bca90c1b7703', 'path': 'identity/common'},
    'common4j': {'profile_id': '60bca90c1b7703', 'path': 'identity/common4j'}
}

HEADERS = {
    'Content-Type': 'application/xml',
}


if len(sys.argv) == 3 and sys.argv[1] in CONFIG.keys():
    prj = sys.argv[1]
    prjVersion = sys.argv[2]
else:
    sys.exit("Invalid argv {}".format(sys.argv))

credentialsdirectory = os.environ.get("CREDENTIALS_SECUREFILEPATH", None)
if not credentialsdirectory:
    sys.exit("Missing CREDENTIALS_SECUREFILEPATH")

with open(credentialsdirectory) as f:
    credentials = json.load(f)

data = '<promoteRequest><data><description>{}</description></data></promoteRequest>'.format(prj)
response = requests.post('https://oss.sonatype.org/service/local/staging/profiles/{}/start'.format(CONFIG[prj]['profile_id']), headers=HEADERS, data=data, verify=False, auth=(credentials["username"], credentials["password"]))
xmldoc = minidom.parseString(response.content)
repository_id = xmldoc.getElementsByTagName('stagedRepositoryId')[0].firstChild.nodeValue
directory = '{}/{}/{}'.format(os.environ["SYSTEM_ARTIFACTSDIRECTORY"], prj, prjVersion)

for filename in os.listdir(directory):
    repo_name = filename
    if filename.startswith("pom-default.xml"):
        new_string = "{}-{}.pom".format(prj, prjVersion)
        repo_name = filename.replace("pom-default.xml", new_string)
    print(filename)
    url = "https://oss.sonatype.org/service/local/staging/deployByRepositoryId/{}/com/microsoft/{}/{}/{}".format(repository_id, CONFIG[prj]['path'], prjVersion, repo_name)
    print(url)

    with open(directory + "/" + filename, 'rb') as f:
        requests.post(url, data=f, verify=False, auth=(credentials["username"], credentials["password"]))
