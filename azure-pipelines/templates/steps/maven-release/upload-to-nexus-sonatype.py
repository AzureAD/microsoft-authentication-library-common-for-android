# Copyright (c) Microsoft Corporation.
# All rights reserved.
#
# This code is licensed under the MIT License.
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files(the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions :
#
# The above copyright notice and this permission notice shall be included in
# all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
# THE SOFTWARE.

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

gpgdirectory = os.environ.get("BUILD_ARTIFACTSTAGINGDIRECTORY", None)
credentialsdirectory = os.environ.get("CREDENTIALS_SECUREFILEPATH", None)
if not credentialsdirectory:
    sys.exit("Missing CREDENTIALS_SECUREFILEPATH")

with open(credentialsdirectory) as f:
    credentials = json.load(f)

data = '<promoteRequest><data><description>{}</description></data></promoteRequest>'.format(prj)
response = requests.post('https://oss.sonatype.org/service/local/staging/profiles/{}/start'.format(CONFIG[prj]['profile_id']), headers=HEADERS, data=data, verify=False, auth=(credentials["username"], credentials["password"]))
print(response.content)
xmldoc = minidom.parseString(response.content)
repository_id = xmldoc.getElementsByTagName('stagedRepositoryId')[0].firstChild.nodeValue

for filename in os.listdir(gpgdirectory):
    repo_name = filename
    if filename.startswith("pom-default.xml"):
        new_string = "{}-{}.pom".format(prj, prjVersion)
        repo_name = filename.replace("pom-default.xml", new_string)
    print(filename)
    url = "https://oss.sonatype.org/service/local/staging/deployByRepositoryId/{}/com/microsoft/{}/{}/{}".format(repository_id, CONFIG[prj]['path'], prjVersion, repo_name)
    print(url)

    with open(gpgdirectory + "/" + filename, 'rb') as f:
        requests.post(url, data=f, verify=False, auth=(credentials["username"], credentials["password"]))
