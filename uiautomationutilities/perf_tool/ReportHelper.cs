//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

using System;
using System.IO;
using System.Net;
using System.Net.Mail;
using System.Net.Mime;
using System.Text;

namespace PerfClTool
{
    internal class ReportHelper
    {
        public struct TableResultSummary
        {
            public int PassCount;
            public int FailCount;
        }

        /// <summary>
        ///     This Method takes the Original HTML Blob and adds the entries in a CSV file as a Table in the HTML blob
        /// </summary>
        /// <param name="result">HTML blob</param>
        /// <param name="CSVFile">Path to the CSV File</param>
        public static TableResultSummary putCSVinHTMLTable(ref StringBuilder result, string CSVFile)
        {
            var TableResultSummary = new TableResultSummary();
            var totalPass = 0;
            var totalFail = 0;
            const char token = ',';


            var lines = File.ReadAllLines(CSVFile);
            result.Append(
                "\n<table border=1 BORDERCOLOR=BLUE cellpadding=10 cellspacing=10 bgcolor=#F2F2F2 style=\"border-collapse: collapse; \">\n");
            var count = 0;
            //string row;
            string headerRow = "";
            string lessImportantScenarios = "";
            string importantScenarios = "";
            foreach (var line in lines)
            {
                var parts = line.Split(token);
                var partsToOutput = new String[parts.Length - 1];
                Array.Copy(parts, partsToOutput, parts.Length - 1);
                if (count++ == 0)
                {
                    //Header row
                    headerRow = "<tr bgcolor=#E5DFEC><td><b><FONT FACE=\"Calibri\">" +
                          string.Join("</FONT></b></td><td><b><FONT FACE=\"Calibri\">", partsToOutput) +
                          "</FONT></b></td></tr>\n";
                }

                else if (parts[parts.Length - 1].Equals( "true", StringComparison.InvariantCultureIgnoreCase))
                {
                    //main scenario
                    importantScenarios = importantScenarios + "<tr><td>" + string.Join("</td><td><FONT FACE=\"Calibri\">", partsToOutput) + "</FONT></td></tr>\n";
                }
                else if (parts[parts.Length - 1].Equals("false",StringComparison.InvariantCultureIgnoreCase))
                {
                    lessImportantScenarios = lessImportantScenarios + "<tr bgcolor=#fdfcff><td>" + string.Join("</td><td><FONT FACE=\"Calibri\">", partsToOutput) + "</FONT></td></tr>\n";
                }
            }
            result.Append(headerRow);
            result.Append(importantScenarios);
            result.Append(lessImportantScenarios);

            result.Append("</table>\n");
            TableResultSummary.PassCount = totalPass;
            TableResultSummary.FailCount = totalFail;
            return TableResultSummary;
        }
        public static void ShowResultNSendEmail(string stringhtml, string fromAddress, string fromPassword, string toList)
        {
            try
            {
                //var resourceDir = Path.Combine(PerfJobContext.GetInstance().ExeDir, "Resources"); Changed here
                // string phoneImagePath;//Changed here commented
                //string specificphoneImagePath = Path.Combine(resourceDir, AndroidDevice.DeviceModel + ".jpg"); Changed here

                /*if (File.Exists(specificphoneImagePath))
                {
                    phoneImagePath = specificphoneImagePath;
                }
                else
                {
                    phoneImagePath = Path.Combine(resourceDir, "android.jpg");
                }


                string samplePerfReportPath = Path.Combine(resourceDir, "SampleAndroidPerfReport.html");

                var teamLogoImagePath = Path.Combine(resourceDir, "TeamLogo.jpg");*/ //Changed here commented this block

                var result = new StringBuilder();
                //var samplePerfReport = File.ReadAllText(samplePerfReportPath).Replace("</html>", ""); //Changed here commented
                // result.Append(samplePerfReport);//Changed here commented

                var TableHeading = "<p><b><FONT COLOR=\"009900\">{0}</FONT></b></P>";

                result = result.Append(string.Format(TableHeading, "Response Time Summary :"));

                //var trs = putCSVinHTMLTable(ref result, PerfJobContext.GetInstance().ResponseTimeSummaryCsvFile);//Changed here commented

                result = result.Append(string.Format(TableHeading,
                    "Resident Memory Size - RSS(KB) at the end of Scenario : "));
                //putCSVinHTMLTable(ref result, PerfJobContext.GetInstance().RssEndSummaryCsvFile);//Changed here commented

                result = result.Append(string.Format(TableHeading,
                    "Virtual Memory Size - VSS(KB) at the end of Scenario : "));
                //putCSVinHTMLTable(ref result, PerfJobContext.GetInstance().VssEndSummaryCsvFile);//Changed here commented

                result.Append("<br/><br/><p style=\"border: dotted 0.5px; padding: 5px; background - color : #fffee8;\"> <strong>Note: </strong> Please check <strong>75BestAvg</strong> number for doing any comparisons between checkpoints.<br/> <strong>75BestAvg</strong> is the average of the subset consisting of 75% of the iterations, that has minimum standard deviation. <strong>75BestAvg</strong> removes any outliers that are deviating too much from other values. Please also make sure that <strong>75BestStdev</strong>(standard deviation of 75Best subset) is within acceptable limits. If its too high, please repeat the runs.</p>");
                result.Append(
                    "<p>Disclaimer: This is an automated Perf Report generated by OXO Engineering Team. Please contact <a href=\"mailto:araggarw@mcirosoft.com\">araggarw@microsoft.com</a> for any issues.</p>");


                result.Append("</html>");

                /*if (PerfJobContext.GetInstance().ApkCheckPoint == PerfJobContext.GetInstance().Tag)
                {
                    result = result.Replace("bnplaceholder", PerfJobContext.GetInstance().ApkCheckPoint);
                }
                else
                {
                    result = result.Replace("bnplaceholder", PerfJobContext.GetInstance().ApkCheckPoint + "(" + PerfJobContext.GetInstance().Tag + ")");
                }

                result = result.Replace("bnplaceholder", PerfJobContext.GetInstance().ApkCheckPoint + "(" + PerfJobContext.GetInstance().Tag + ")");
                result = result.Replace("buildpathplaceholder", PerfJobContext.GetInstance().UserSuppliedApkPath.Trim());
                result = result.Replace("androiddeviceplaceholder", AndroidDevice.DeviceModel + "(" + AndroidDevice.AndroidVersion + ")");
                result = result.Replace("logsdirplaceholder", PerfJobContext.GetInstance().RemoteLogsDirectory);
                result = result.Replace("runidplaceholder", PerfJobContext.GetInstance().TaskId);
                result = result.Replace("androididplaceholder", AndroidDevice.AndroidId);
                result = result.Replace("guidplaceholder", AndroidDevice.AndroidIdGuid);
                result = result.Replace("deviceserialnoplaceholder", AndroidDevice.DeviceId);

                var perfResultSummaryHtml = Path.Combine(PerfJobContext.GetInstance().LocalLogsBaseDir,"..", "PerfResultSummary.html");*///Changed here commented

                /*using (var sw = new StreamWriter(perfResultSummaryHtml, false))
                {
                    sw.WriteLine(result.ToString());
                }*/ //Changed here commented

                //Fix the images for the email

                result = result.Replace("android.jpg", "cid:Pic1");
                result = result.Replace("TeamLogo.jpg", "cid:Pic2");

                //var htmlBody = result.ToString();//Changed here commented
                var avHtml = AlternateView.CreateAlternateViewFromString
                    (stringhtml, null, MediaTypeNames.Text.Html);

                // Create a LinkedResource object for each embedded image
                /*var pic1 = new LinkedResource(phoneImagePath, MediaTypeNames.Image.Jpeg);
                pic1.ContentId = "Pic1";
                avHtml.LinkedResources.Add(pic1);


                var pic2 = new LinkedResource(teamLogoImagePath, MediaTypeNames.Image.Jpeg);
                pic2.ContentId = "Pic2";
                avHtml.LinkedResources.Add(pic2);*/ //Changed here commented

                var msg = new MailMessage();
                //msg.From = new MailAddress(Environment.UserName + "@microsoft.com", Environment.UserName);
                msg.From = new MailAddress(fromAddress);
                //msg.To.Add(Environment.UserName + "@microsoft.com");
                msg.To.Add(toList);
                msg.IsBodyHtml = true;
                /*msg.Subject = String.Format("Automated Perf Report : {0} : {1} : {2}({3})",
                    PerfJobContext.GetInstance().OfficeApplication.GetShortName(), PerfJobContext.GetInstance().Tag, AndroidDevice.DeviceModel, AndroidDevice.AndroidVersion);*/ //Changed here commented
                msg.Subject = "Perf report";

                //msg.Body = result.ToString();
                msg.AlternateViews.Add(avHtml);

                //Attachments
                // msg.Attachments.Add(new Attachment(PerfJobContext.GetInstance().DetailedResultsCsvFile));//Changed here commented

                //var mailClient = new SmtpClient("smtphost");
                //mailClient.Credentials = CredentialCache.DefaultNetworkCredentials;
                var smtp = new SmtpClient("smtp-mail.outlook.com");
                smtp.Credentials = new NetworkCredential(fromAddress, fromPassword);
                smtp.EnableSsl = true;
                smtp.DeliveryMethod = System.Net.Mail.SmtpDeliveryMethod.Network;
                try
                {
                    smtp.Send(msg);
                }
                catch (Exception e)
                {
                    Console.WriteLine(e.StackTrace);
                }

            }
            catch (Exception e)
            {
                Console.WriteLine(e.StackTrace);
            }
        }
    }
}
