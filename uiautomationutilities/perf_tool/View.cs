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

using System.Collections.Generic;
using System.Text;
using System;
using System.Globalization;

namespace PerfDiffResultMailer
{
    /// <summary>
    /// This class represents the email report look/display
    /// </summary>
    class View
    {
        public static string BuildHeaderofHTML()
        {
            StringBuilder htmlBuilder = new StringBuilder();
            //Create Top Portion of HTML Document
            htmlBuilder.Append("<html>");
            htmlBuilder.Append("<head>");
            htmlBuilder.Append("<title>");
            htmlBuilder.Append("PerfReport");
            htmlBuilder.Append("</title>");
            htmlBuilder.Append("</head>");
            htmlBuilder.Append("<body>");
            htmlBuilder.Append(CSSString());
            return htmlBuilder.ToString();
        }
        public static string BuildEndOfHTML()
        {
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.Append("</body>");
            htmlBuilder.Append("</html>");
            return htmlBuilder.ToString();
        }
        public static string CSSString() //CSS requirements
        {
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.Append(" <style type=\"text/css\" media=\"all\">");
            htmlBuilder.Append(".color1{ background-color:#f0f8ff } ");
            htmlBuilder.Append(".blue{ background-color:#add8e6 } ");
            htmlBuilder.Append(".pink{ background-color:#ffdab9}  ");
            htmlBuilder.Append(".lemon{ background-color:#EBE64E}  ");
            htmlBuilder.Append(".seagreen{ background-color:#4EEB98}  ");
            htmlBuilder.Append(".lightgreen{ background-color:#A1EB4E}  ");
            htmlBuilder.Append(".red{ background-color:#ff0000} .yellow{background-color:#FAD7A0} ");
            htmlBuilder.Append(".green{background-color:#228b22} .purple{background-color:#D2B4DE } " +
                ".grey{background-color:#B2BABB}.lightgrey{background-color:#ECF0F1}");
            htmlBuilder.Append("table {border-spacing:0; BORDER:black 1px solid; BORDER-COLLAPSE:collapse}");
            htmlBuilder.Append("table td{ BORDER:black 1px solid;padding:0; margin:0;}");
            htmlBuilder.Append("</style>");
            return htmlBuilder.ToString();
        }

        internal static string CreateTableHtml(List<Parameter> parameters, 
            HashSet<string> primaryMeasurements, HashSet<string> secondaryMeasurements,
            string[] heading, HashSet<string> activeScenarios, HashSet<string> activeM, string jobId, string runName)
        {
            StringBuilder htmlBuilder = new StringBuilder();
            int columns = (parameters.Count * (3)) + 2;
            htmlBuilder.Append("<table class=\"table\">");
            htmlBuilder.Append(CreateTableHeader(columns, parameters, heading, jobId, runName));
            htmlBuilder.Append(DiffTableHTML1(parameters, "grey", activeScenarios, primaryMeasurements, secondaryMeasurements));
            htmlBuilder.Append("</table>");
            htmlBuilder.Append("<br><br>");
            return htmlBuilder.ToString();
        }

        private static string DiffTableHTML1(List<Parameter> parameters, string color, 
            HashSet<string> activeScenarios, HashSet<string> primaryMeasurements, HashSet<string> secondaryMeasurements)
        {
            StringBuilder htmlBuilder = new StringBuilder();
            foreach (string scenario in activeScenarios)
            {
                HashSet<string> activeM = new HashSet<string>();
                foreach (var parameter in parameters)
                {
                    if (parameter.BaseScenarioToPerfValueMap.ContainsKey(scenario))
                    {
                        foreach (var perfM in parameter.BaseScenarioToPerfValueMap[scenario])
                        {
                            activeM.Add(perfM.Key);
                        }
                    }

                    if (parameter.TargetScenarioToPerfValueMap.ContainsKey(scenario))
                    {
                        foreach (var perfM in parameter.TargetScenarioToPerfValueMap[scenario])
                        {
                            activeM.Add(perfM.Key);
                        }
                    }
                }
                bool first = true;
                htmlBuilder.Append("<tr  class=\"" + color + "\">");
                htmlBuilder.Append("<td class=\"" + "blue" + "\"rowspan=\"" + /*1*/activeM.Count +
                    "\"style=\"width:90px\" style=\"padding-left:5px; padding-right:5px;\">" + scenario + "</td>");
                foreach (string measurement in activeM)
                {
                    string mColor = (primaryMeasurements.Contains(measurement)) ? "grey" : "lightgrey";

                    if (!first)
                        htmlBuilder.Append("<tr class=\"" + mColor + "\">");
                    htmlBuilder.Append("<td rowspan=\"" + 1 + "\"style=\"width:290px\" " +
                        "style=\"padding-left:5px; padding-right:5px;\" >" + measurement + "</td>");
                    foreach (var parameter in parameters)
                    {
                        Dictionary<string, double> baseMeasurements = new Dictionary<string, double>();
                        if (parameter.BaseScenarioToPerfValueMap.ContainsKey(scenario))
                        {
                            baseMeasurements = parameter.BaseScenarioToPerfValueMap[scenario];
                        }
                        Dictionary<string, double> targetMeasurements = new Dictionary<string, double>();

                        if (parameter.TargetScenarioToPerfValueMap.ContainsKey(scenario))
                        {
                            targetMeasurements = parameter.TargetScenarioToPerfValueMap[scenario];
                        }


                        htmlBuilder.Append(DiffTableHelper(baseMeasurements, targetMeasurements, measurement, parameter.Threshhold));
                        first = false;


                    }
                    htmlBuilder.Append("</tr>");
                }
            }
            return htmlBuilder.ToString();
        }

        // differences display 
        private static string DiffTableHelper(Dictionary<string, double> BaseScenarioToPerfValueMap, 
            Dictionary<string, double> TargetScenarioToPerfValueMap, string scenario, int threshhold)
        {
            StringBuilder htmlBuilder = new StringBuilder();
            bool basePresent = false;
            bool targetPresent = false;
            double baseValue = 0;
            double targetValue = 0;
            if (BaseScenarioToPerfValueMap.ContainsKey(scenario))
            {
                basePresent = true;
                baseValue = BaseScenarioToPerfValueMap[scenario];
                string value = String.Format(CultureInfo.InvariantCulture, "{0:#,#.00}", baseValue);
                htmlBuilder.Append("<td align=\"right\" style=\"padding-right:5px;\">" + value + "</td>");
            }
            else
            {
                htmlBuilder.Append("<td></td>");
            }

            if (TargetScenarioToPerfValueMap.ContainsKey(scenario))
            {
                targetPresent = true;
                targetValue = TargetScenarioToPerfValueMap[scenario];
                string value = String.Format(CultureInfo.InvariantCulture, "{0:#,#.00}", targetValue);
                htmlBuilder.Append("<td align=\"right\" style=\"padding-right:5px;\">" + value + "</td>");
            }
            else
            {
                htmlBuilder.Append("<td></td>");
            }

            if (basePresent && targetPresent)
            {
                double diff = targetValue - baseValue;
                // Calculate color based on %. i.e. if the value deviates +- threshold% from the base then give the color
                double diffpercent = (((diff > 0) ? diff : (-1 * diff)) / baseValue) * 100;
                diffpercent = (diff > 0) ? diffpercent : (-1 * diffpercent);
                string diffColor = GetColor(diffpercent, threshhold);
                string difference = String.Format(CultureInfo.InvariantCulture, "{0:#,#.00}", diff);
                htmlBuilder.Append("<td class=\"" + diffColor +
                                   "\" align=\"right\" style=\"padding-right:5px;\">" + difference +
                                   "</td>");
            }
            else
            {
                htmlBuilder.Append("<td></td>");
            }
            return htmlBuilder.ToString();
        }

        internal static string EndofJobDetailsTable()
        {
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.Append("</table>");
            htmlBuilder.Append("<br>");
            return htmlBuilder.ToString();
        }

        private static string GetTaskInfoTable(List<Task> tasks, string color)
        {
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.Append("<table cellspacing=\"0\" cellpadding=\"0\">");
            foreach (Task task in tasks)
            {
                htmlBuilder.Append("<tr>");
                htmlBuilder.Append("<td style=\"padding-left:5px; padding-right:5px;border: none;\">" + 
                    "<a href=\"" + task.LogsDir + "\"><u> (" + task.Checkpoint + ") </u></a></td>");
                string featuregateoverrides = "";
                foreach (var featuregate in task.FeatureGateOverrides)
                {
                    featuregateoverrides += featuregate.Key + "|" + featuregate.Value + " , ";
                }
                featuregateoverrides = featuregateoverrides.TrimEnd(',', ' ');
                htmlBuilder.Append("</tr>");
            }
            htmlBuilder.Append("</table>");
            return htmlBuilder.ToString();
        }

        public static string ReportSummaryHeaderValues(List<Task> baseTasks, List<Task> latestTasks, List<Task> targetTasks, string app, string deviceModel, string OS)
        {
            StringBuilder htmlBuilder = new StringBuilder();

            htmlBuilder.Append("<tr>");
            htmlBuilder.Append("<td style=\"font-weight:bold\" style=\"padding-left:5px; padding-right:5px;\" align=\"left\" class=\"blue\">" + app + "</td>");
            htmlBuilder.Append("<td style=\"font-weight:bold\" style=\"padding-left:5px; padding-right:5px;\" align=\"left\" class=\"blue\">" + deviceModel + " with " + OS + "</td>");
            htmlBuilder.Append("<td>");
            htmlBuilder.Append(GetTaskInfoTable(baseTasks, "blue"));
            htmlBuilder.Append("</td>");
            htmlBuilder.Append("<td>");
            htmlBuilder.Append(GetTaskInfoTable(latestTasks, "blue"));
            htmlBuilder.Append("</td>");
            htmlBuilder.Append("<td>");
            htmlBuilder.Append(GetTaskInfoTable(targetTasks, ""));
            htmlBuilder.Append("</td>");
            return htmlBuilder.ToString();
        }

        // Measurement table headers
        private static string CreateTableHeader(int columns, List<Parameter> parameters, string[] heading, string jobId, string runName)
        {
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.Append("<tr class=\"blue\" style=\"font-weight:bold\"><td colspan=\"" + /*1*/2 + "\" align=\"center\">" + "Perf Run " + jobId +" vs "+ runName+ " run</td>");
            foreach (var parameter in parameters)
            {
                htmlBuilder.Append("<td colspan=\"" + 3 + "\" align=\"center\" class=\"" + parameter.Color + "\">" + parameter.Name + "</td>"); //add indivdual colors
            }
            htmlBuilder.Append("</tr>");
            htmlBuilder.Append("<tr class=\"yellow\" style=\"font-weight:bold\">"); // Scenario / Measurement Name
            htmlBuilder.Append("<td colspan=\"" + 1 + "\"align=\"center\"> Scenario Name </td>");
            htmlBuilder.Append("<td colspan=\"" + 1 + "\"align=\"center\"> Measurement Name </td>");
            foreach (var parameter in parameters)
            {
                htmlBuilder.Append("<td style=\"padding-left:5px; padding-right:5px;\"><a href = \"" + parameter.BaseLogDir + "\">" + parameter.BaseCheckpoint + "</a></td>");
                htmlBuilder.Append("<td style=\"padding-left:5px; padding-right:5px;\"><a href = \"" + parameter.TargetLogDir + "\">" + parameter.TargetCheckPoint + "</a></td>");
                htmlBuilder.Append("<td style=\"padding-left:5px; padding-right:5px;\">" + "Difference" + "</td>");
            }
            htmlBuilder.Append("</tr>");
            return htmlBuilder.ToString();
        }

        // generate a color based on the diff and threshold
        private static string GetColor(double diff, double threshold)
        {
            if (diff > threshold)
                return "red";
            else if (diff < -1 * threshold)
                return "green";
            else
                return "";
        }

        /// <summary>
        /// Formatting the report summary values
        /// </summary>
        /// <param name="featuregatesToTasksValueMap"></param>
        /// <param name="baseTasks"></param>
        /// <param name="targetTasks"></param>
        /// <param name="appName"></param>
        /// <returns></returns>
        public static string CreateSummaryHtml(Dictionary<string, Dictionary<Task, string>> featuregatesToTasksValueMap,
            List<Task> baseTasks, List<Task> targetTasks, string appName)
        {
            StringBuilder htmlBuilder = new StringBuilder();
            if (featuregatesToTasksValueMap.Count == 0)
                htmlBuilder.Append("<p><bold>The feature gates are identical for all checkpoints for " + appName + ".<bold></p>");
            else
            {
                htmlBuilder.Append("<br><table class=\"table\">");
                htmlBuilder.Append("<thead><tr class=\"pink\" style=\"font-weight:bold\"><td align=\"center\"colspan=\""
                                   + (baseTasks.Count + targetTasks.Count + 1) + "\">" + appName + "</td></tr></thead>");
                htmlBuilder.Append("<tr class=\"blue\" style=\"font-weight:bold\">");
                htmlBuilder.Append("<td style=\"padding-left:5px; padding-right:5px;\">Feature Gates</td>");
                foreach (Task baseTask in baseTasks)
                {
                    htmlBuilder.Append("<td style=\"padding-left:5px; padding-right:5px;\" align=\"center\">" + baseTask.Checkpoint +
                                       "<a href=\"" + baseTask.LogsDir + "\"><u> (" + baseTask.Id + ") </u></a></td>");
                }
                foreach (Task targetTask in targetTasks)
                {
                    htmlBuilder.Append("<td style=\"padding-left:5px; padding-right:5px;\" align=\"center\">" + targetTask.Checkpoint +
                                       "<a href=\"" + targetTask.LogsDir + "\"><u> (" + targetTask.Id + ") </u></a></td>");
                }
                htmlBuilder.Append("</tr>");
                foreach (var featuregateTocheckpointValueMap in featuregatesToTasksValueMap)
                {
                    htmlBuilder.Append("<tr>");
                    htmlBuilder.Append("<td style=\"padding-left:5px; padding-right:5px;\">" + featuregateTocheckpointValueMap.Key + "</td>");
                    Dictionary<Task, string> tasksToValue = featuregateTocheckpointValueMap.Value;
                    foreach (Task baseTask in baseTasks)
                    {
                        if (tasksToValue.ContainsKey(baseTask))
                        {
                            string value = tasksToValue[baseTask];
                            if (value.Length > 40)
                            {
                                value = value.Substring(0, 40) + "...";
                            }
                            htmlBuilder.Append("<td align=\"center\">" + value + "</td>");
                        }
                        else
                        {
                            htmlBuilder.Append("<td align=\"center\">-</td>");
                        }
                    }
                    foreach (Task targetTask in targetTasks)
                    {
                        if (tasksToValue.ContainsKey(targetTask))
                        {
                            string value = tasksToValue[targetTask];
                            if (value.Length > 40)
                            {
                                value = value.Substring(0, 40) + "...";
                            }
                            htmlBuilder.Append("<td align=\"center\">" + value + "</td>");
                        }
                        else
                        {
                            htmlBuilder.Append("<td align=\"center\">-</td>");
                        }
                    }
                    htmlBuilder.Append("</tr>");
                }
                htmlBuilder.Append("</table>");
            }

            return htmlBuilder.ToString();
        }

        public static List<string> ResultInit()
        {
            List<string> result = new List<string>();
            result.Add(BuildHeaderofHTML());
            return result;
        }

        /// <summary>
        /// Formatting the report summary headers
        /// </summary>
        /// <returns></returns>
        public static List<string> ReportSummaryHeaders()
        {
            List<string> info = new List<string>();
            info.Add("<p><font color=\"red\" size=\"+2\"><bold>Job Summary<bold></font></p>");
            StringBuilder htmlBuilder = new StringBuilder();

            htmlBuilder.Append("<table class=\"table\">");
            htmlBuilder.Append("<tr class=\"pink\" style=\"font-weight:bold\"  align=\"center\">");
            htmlBuilder.Append("<td style=\"padding-left:5px; padding-right:5px;\">Target Application</td>");
            htmlBuilder.Append("<td style=\"padding-left:5px; padding-right:5px;\">Device Configuration</td>");
            htmlBuilder.Append("<td style=\"padding-left:5px; padding-right:5px;\">Base Build</td>");
            htmlBuilder.Append("<td style=\"padding-left:5px; padding-right:5px;\">Latest Build</td>");
            htmlBuilder.Append("<td style=\"padding-left:5px; padding-right:5px;\">Target Build</td>");

            htmlBuilder.Append("</tr>");
            info.Add(htmlBuilder.ToString());
            return info;
        }

        public static List<string> SummaryInit()
        {
            List<string> summary = new List<string>();
            summary.Add("<br><p><font color=\"red\" size=\"+2\"><bold>Feature gates Summary<bold></font></p>");
            return summary;
        }
    }
}
