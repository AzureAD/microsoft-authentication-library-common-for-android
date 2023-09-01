// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.java.util;

import com.microsoft.identity.common.java.challengehandlers.PKeyAuthChallenge;

import org.junit.Assert;
import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

public class UrlUtilTest {

    @Test
    public void testPathWithTrailingSlashes() {
        Assert.assertEquals(
                "https://www.test.com/path/another/path",
                UrlUtil.removeTrailingSlash("https://www.test.com/path/another/path//")
        );
    }

    @Test
    public void testPathWithTrailingSlash() {
        Assert.assertEquals(
                "https://www.test.com/path/another/path",
                UrlUtil.removeTrailingSlash("https://www.test.com/path/another/path/")
        );
    }

    @Test
    public void testPathWithNoTrailingSlash() {
        Assert.assertEquals(
                "https://www.test.com/path/another/path",
                UrlUtil.removeTrailingSlash("https://www.test.com/path/another/path")
        );
    }

    @Test
    public void testAppendEmptyPathUrl() throws MalformedURLException, URISyntaxException {
        Assert.assertEquals(new URL("https://www.test.com"),
                UrlUtil.appendPathToURL(new URL("https://www.test.com"), null));

        Assert.assertEquals(new URL("https://www.test.com"),
                UrlUtil.appendPathToURL(new URL("https://www.test.com"), ""));
    }

    @Test
    public void testAppendPathStringWithExtraSlashes() throws MalformedURLException, URISyntaxException {
        Assert.assertEquals(new URL("https://www.test.com/this/is/a/path"),
                UrlUtil.appendPathToURL(new URL("https://www.test.com"), "//this/is/a//path"));
    }

    @Test
    public void testAppendPathStringWithoutStartingSlash() throws MalformedURLException, URISyntaxException {
        Assert.assertEquals(new URL("https://www.test.com/path"),
                UrlUtil.appendPathToURL(new URL("https://www.test.com"), "path"));
    }

    @Test
    public void testAppendPathStringToUrlWithPath() throws MalformedURLException, URISyntaxException {
        Assert.assertEquals(new URL("https://www.test.com/path/another/path"),
                UrlUtil.appendPathToURL(new URL("https://www.test.com/path"), "/another/path"));
    }

    @Test
    public void testGettingParameterFromUrl() throws URISyntaxException {
        final Map<String, String> queryParams = UrlUtil.getParameters(
                new URI("https://www.test.com/path/another/path?param1=value1&param2=value2"));
        Assert.assertEquals(2, queryParams.size());
        Assert.assertEquals("value1", queryParams.get("param1"));
        Assert.assertEquals("value2", queryParams.get("param2"));
    }

    @Test
    public void testGettingParameterFromUrlContainingUrl() throws URISyntaxException {
        final Map<String, String> queryParams = UrlUtil.getParameters(
                new URI("msauth://com.msft.identity.client.sample.local/1wIqXSqBj7w%2Bh11ZifsnqwgyKrY%3D?wpj=1&username=test%40test.onmicrosoft.com&app_link=https%3a%2f%2fplay.google.com%2fstore%2fapps%2fdetails%3fid%3dcom.azure.authenticator"));
        Assert.assertEquals(3, queryParams.size());
        Assert.assertEquals("1", queryParams.get("wpj"));
        Assert.assertEquals("test@test.onmicrosoft.com", queryParams.get("username"));
        Assert.assertEquals("https://play.google.com/store/apps/details?id=com.azure.authenticator", queryParams.get("app_link"));
    }

    @Test
    public void testGettingParameterFromEmptyUrl() throws URISyntaxException {
        final Map<String, String> queryParams = UrlUtil.getParameters(new URI(""));
        Assert.assertEquals(0, queryParams.size());
    }

    @Test
    public void testGettingParamsFromPKeyAuthRedirectUrl() throws URISyntaxException {
        final Map<String, String> queryParams = UrlUtil.getParameters(
                new URI("urn:http-auth:PKeyAuth?CertAuthorities=OU%3d82dbaca4-3e81-46ca-9c73-0950c1" +
                        "eaca97%2cCN%3dMS-Organization-Access%2cDC%3dwindows%2cDC%3dnet&Version=1.0&Con" +
                        "text=rQIIAa2RPW_TUBiFfeMkJIGKwADdyNAKRHWd6287EhJpKW1CPwKholSi4ca-Tm6xfRPbSZr-A" +
                        "iSWiqEDC1In6ALqhJi6wJApYkKMjJ0Yq064VP0HnOG8z3D0DufkUqKgCuZdXhJQaUppIrtpWBI0HUm" +
                        "DiiLq0GjaBMoOIrKmao5BUHA9l__2MZx58Wlz-c17__TlzQ_0AKx6Ie5F7VKxaDFP8EInEqhN_IhGQ" +
                        "8FyaUxCiL2OSwSXWdgtioNKd73end3SB9PSbFsUN6gT-t1Ba_goeD4tP_gCwBiAtwlunAC_EldXy_F" +
                        "z6cxYQHfIcSLlshb13_EroiIrJUW1LV0lMrQVy4IKlkyITdWAGrJ0kSgGlkQZYlXTSNNAECEzDjkmh" +
                        "k3ViJMIYVE0ka3L2gE_hc4lwzM_N-uCLnTIT7CghX26gyPK_HDE3-6FJBACgu0C6xCfxsdxXOqTBrY" +
                        "sEoaFTsAc6pKfPPjN36K2i5vifS_8B4rAfI9aAQtZXFvc324SjJPgOHkjA_KpyWyBu3MNgWomw-czk" +
                        "1yBO0mC_VS8wo_9YW9zz1_Zq2WP-t8rYJQqDlib9aou6W_0VxcGdVIlc1X2eHFN8_qvhtFaz1NmFtu" +
                        "1LXP7WeUeKom7aXCYTma4PBilryzXy0tC2bcDRu0_6cTrS9zX7P-e9STLHeRmljyDPo2Wd_SF2jzS1" +
                        "zfmn6hdVPEb64b0sLGt1BGsG_acxDq1yuccGF8GRxPcXw2.AQABAAEAAAD--DLA3VO7QrddgJg7Wev" +
                        "rzjjKTTYtfoOywttRhaDB_GjGCDVgGLgYq25ghx2_nsx8f7P27pSchQJObGQhayGVeHn5rf7Sih-zg" +
                        "0Gbgn67s1CmtT2MLY1Z7sHFELc6AYnHlKNK6u6PC5tNUTHvn-I7QdOpT_5CUNX9mmmiM9Yty-v0szK" +
                        "0mLpa4F0oL_cRaNVJvM_X2IUI-ljh6O9BxuWRiAoDUfwXb04MWGHKa1fCtT-8WA3YbuwsTeO1Z6nSB" +
                        "8g29avnwFBYxNCfnwEKkm-1gNLTSWkeeE7SWZjUXDReM5WGp1h3ze5ujB241lEbpu1GlLSXG9doA9S" +
                        "to_6uO8MvuRZ0GwOwIEPd1FRzHYCfddE9rdBEEdXUjS1Ta5rt3SKY8bSRsO_B4lGDonb5DNY9huKrc" +
                        "09d2GmO6UxkYlD2qjMu4KGo1446FV6clwwdSrU28Dm0DEggKBqjuCFNEb4PC_lbxbZLKh8vniC1HnA" +
                        "ouVtMNEQe5uUGRjm268ePDeuZL5d2R2gNcNsutsFmPtQoTKZWuYCUKS3aJU34dD3y0Z988SXABtpLn" +
                        "M8WjA6jhlUxP9P01TdRWKPxaPGtp3Ig22zg7AabQPq69TfRsIIE_31fY8TKEZ9Bo-Ncb5NgBmdyIhM" +
                        "jQkyh62xsJFdloLojSTs5XBVty8g_0zT06lMgs41pMnS3rKikcy2uJdopdwTpFZulDA4ADmGImN1Yv" +
                        "VhN06YqG3EFArq12QLb0YyB3KeGlvsnJEUK5aKGVAig9MkgAA&nonce=3s0TxehKjHGlSXutlbmiCU" +
                        "cJfCa2vb_iqvnw85c8dUo&SubmitUrl=https%3a%2f%2flogin.microsoftonline.com%2fcomm" +
                        "on%2fDeviceAuthPKeyAuth"));
        Assert.assertEquals(5, queryParams.size());
    }

    @Test
    public void testGettingParamsFromOpaqueUrl() throws URISyntaxException {
        final Map<String, String> queryParams = UrlUtil.getParameters(
                new URI("something:test?hello=world?hola=abcd"));
        Assert.assertEquals(1, queryParams.size());
        Assert.assertEquals("world?hola=abcd", queryParams.get("hello"));
    }

    @Test
    public void testGettingParamsFromPKeyAuthSubmitUrl() throws URISyntaxException {
        final Map<String, String> queryParams = UrlUtil.getParameters(
                new URI("urn:http-auth:PKeyAuth?SubmitUrl=https%3a%2f%2ffs.msidlab4.com%3a44" +
                        "3%2fadfs%2fls%2f%3flogin_hint%3dsomeone%2540msidlab4.com%26wfresh%3d0%26" +
                        "client-request-id%3d247af5dc-1234-4601-ba39-1727b5c17a17%26username%3d" +
                        "someone%2540msidlab4.com%26wa%3dwsignin1.0%26wtrealm%3durn%253afederation" +
                        "%253aMicrosoftOnline%26wctx%3destsredirect%253d2%2526estsrequest%253drQ" +
                        "IIAa2Ru2_1234-MkTUwxpSBatgi1AoGu4-tr38SRkOrSB32EIJVCyYL8uI4Ntm9qOzXNX" +
                        "8BYscHIRieEhIQqBlg7dUSFwSPqQhekIjGQIlY2lvOd4ZPO7-jwRSSqonadk0WpOaWYkm02" +
                        "LBlqjkygoqA6bJg2hdiRKCYqcRpUisb5sa8_BlM_t8DCnjZx9d2nwcQuaAex0U_cZq1msUA" +
                        "MYicRPZuGiZdsi5bvDZ0YG0HPp6LPLMOvoXRpc2Ntc_ZxPZ2WZ12EOp4Th5tpd3slejiN59" +
                        "4DcADANwCOAXiev9D1bN8wZ4L4z1XEYchBHhzmz7X1Yap8KizyBvQoX_RZ1wtfci2ZoCYxK" +
                        "R1Cq9CwNAyVOjKheVqKaJbVwIZs2xqCDsHIIkSBjtJwoEIMAxpYItAkloXUuo0kW93lLrtJ" +
                        "0ouH_bqR0XPF1AttlsZiSJO33OhfLnzKtc_N_PO1JtrUMfp-UmU9Gnp2lTmO74X0kWFZNI6" +
                        "rvYg5nk8_c2CnAA4K4KhwqQzGipOVau7aeQksl8vcWGUSVHMnBfCqOBxi_nXw60qlqr9Y_P" +
                        "Bl7_vt3H6xVlNWlAdtvMHmmf7EmF3Agb7-lKS3NE9fxi1i-74au4q5cQfpN-tNtFMC-6XR1" +
                        "pq-KuqhHTHPPi6BZyO5nRGwV_nfq-7yN-Yabvu-3x-EfrvD2D3TWA9WPW2xHqUtAreWtjos" +
                        "HbRxf_vusvWGB4f8RVmSEUQSRLgqSU0FN1Wtczha5jk37o8D9PFsLhNAJuQzgcuEQiYUM6G" +
                        "UCSOZUM6ESibwmXDmNw2&nonce=_OT55FxMhmuyOSoUnL1234&Version=1.0&Context=b" +
                        "ccf3220-e6fa-4016-8f9a-b9d9b2aa8986&CertAuthorities=OU%253d5ce5e2c6-123" +
                        "4-4e53-840b-98acf367a25e%252cCN%253dMS-Organization-Access%2b%252cDC%25" +
                        "3dMSIDLAB4%2b%252cDC%253dcom%2b%253bOU%253d82dbaca4-3e81-46ca-9c73-0950" +
                        "c1eaca97%252cCN%253dMS-Organization-Access%2b%252cDC%253dwindows%2b%252" +
                        "cDC%253dnet%2b&client-request-id=247af5dc-1234-4601-ba39-1727b5c17a17#"));
        Assert.assertEquals(6, queryParams.size());
        Assert.assertEquals("https://fs.msidlab4.com:443/adfs/ls/?login_hin" +
                "t=someone%40msidlab4.com&wfresh=0&client-request-id=247af5dc-1234-4601-ba39-1727" +
                "b5c17a17&username=someone%40msidlab4.com&wa=wsignin1.0&wtrealm=urn%3afederation%" +
                "3aMicrosoftOnline&wctx=estsredirect%3d2%26estsrequest%3drQIIAa2Ru2_1234-MkTUw" +
                "xpSBatgi1AoGu4-tr38SRkOrSB32EIJVCyYL8uI4Ntm9qOzXNX8BYscHIRieEhIQqBlg7dUSFwSPqQh" +
                "ekIjGQIlY2lvOd4ZPO7-jwRSSqonadk0WpOaWYkm02LBlqjkygoqA6bJg2hdiRKCYqcRpUisb5sa8_B" +
                "lM_t8DCnjZx9d2nwcQuaAex0U_cZq1msUAMYicRPZuGiZdsi5bvDZ0YG0HPp6LPLMOvoXRpc2Ntc_Zx" +
                "PZ2WZ12EOp4Th5tpd3slejiN594DcADANwCOAXiev9D1bN8wZ4L4z1XEYchBHhzmz7X1Yap8KizyBvQ" +
                "oX_RZ1wtfci2ZoCYxKR1Cq9CwNAyVOjKheVqKaJbVwIZs2xqCDsHIIkSBjtJwoEIMAxpYItAkloXUuo" +
                "0kW93lLrtJ0ouH_bqR0XPF1AttlsZiSJO33OhfLnzKtc_N_PO1JtrUMfp-UmU9Gnp2lTmO74X0kWFZN" +
                "I6rvYg5nk8_c2CnAA4K4KhwqQzGipOVau7aeQksl8vcWGUSVHMnBfCqOBxi_nXw60qlqr9Y_PBl7_vt" +
                "3H6xVlNWlAdtvMHmmf7EmF3Agb7-lKS3NE9fxi1i-74au4q5cQfpN-tNtFMC-6XR1pq-KuqhHTHPPi6" +
                "BZyO5nRGwV_nfq-7yN-Yabvu-3x-EfrvD2D3TWA9WPW2xHqUtAreWtjosHbRxf_vusvWGB4f8RVmSEU" +
                "QSRLgqSU0FN1Wtczha5jk37o8D9PFsLhNAJuQzgcuEQiYUM6GUCSOZUM6ESibwmXDmNw2",
                queryParams.get("SubmitUrl"));
    }
}
