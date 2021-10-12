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
using System.Collections.Generic;

namespace PerfClTool.Measurement
{
    internal class MeasurementsStore
    {
        public static Dictionary<string, List<PerfMeasurementsSet>> AllScenarioMeasurements { get; private set; }
        static MeasurementsStore()
        {
            AllScenarioMeasurements = new Dictionary<string, List<PerfMeasurementsSet>>();
        }

        public static void clear()
        {
            AllScenarioMeasurements = new Dictionary<string, List<PerfMeasurementsSet>>();
        }

        /// <summary>
        /// add this scenario measurements
        /// </summary>
        /// <param name="scenarioName"></param>
        /// <param name="perfData"></param>
        /// <param name="enabledMeasurementsConfiguration"></param>
        public static void AddScenarioIterationMeasurements(String scenarioName, PerfData perfData,
            List<PerfMeasurementConfiguration> enabledMeasurementsConfiguration)
        {
            if (!AllScenarioMeasurements.ContainsKey(scenarioName))
            {
                AllScenarioMeasurements.Add(scenarioName, new List<PerfMeasurementsSet>());
                foreach (var measurementConfiguration in enabledMeasurementsConfiguration)
                {
                    AllScenarioMeasurements[scenarioName].Add(new PerfMeasurementsSet(measurementConfiguration));
                }
            }
            foreach (var measurementSet in AllScenarioMeasurements[scenarioName])
            {
                measurementSet.AddIterationMeasurement(perfData);
            }
        }

        /// <summary>
        /// produce aggregate measurements
        /// </summary>
        public static void GenerateAggregateMeasurements()
        {
            foreach (var measurementSetList in AllScenarioMeasurements.Values)
            {
                //Delete any measurementSet that has no actual data.
                measurementSetList.RemoveAll(x => x._iterationMeasurements.Count == 0);
                measurementSetList.ForEach(t => t.GenerateAggregateMeasurements());
            }
        }

        /// <summary>
        /// save response time summary to file
        /// </summary>
        /// <param name="fileName"></param>
        public static void DumpResponseTimeSummaryToFile(String fileName)
        {
            PerfMeasurementsSet.AppendMeasurementSummaryHeadersToFile(fileName);
            foreach (var scenarioMeasurements in AllScenarioMeasurements)
            {
                AllScenarioMeasurements[scenarioMeasurements.Key].ForEach(t =>
                t.AppendMeasurementSummaryToFile(fileName, scenarioMeasurements.Key, PerformanceMetricType.ResponseTime));
            }
        }

        /// <summary>
        /// save rss end summary to file
        /// </summary>
        /// <param name="fileName"></param>
        public static void DumpRssEndSummaryToFile(String fileName)
        {
            PerfMeasurementsSet.AppendMeasurementSummaryHeadersToFile(fileName);
            foreach (var scenarioMeasurements in AllScenarioMeasurements)
            {
                AllScenarioMeasurements[scenarioMeasurements.Key].ForEach(t =>
                t.AppendMeasurementSummaryToFile(fileName, scenarioMeasurements.Key, PerformanceMetricType.RssEnd));
            }
        }

        /// <summary>
        /// save vss end summary to file
        /// </summary>
        /// <param name="fileName"></param>
        public static void DumpVssEndSummaryToFile(String fileName)
        {
            PerfMeasurementsSet.AppendMeasurementSummaryHeadersToFile(fileName);
            foreach (var scenarioMeasurements in AllScenarioMeasurements)
            {
                AllScenarioMeasurements[scenarioMeasurements.Key].ForEach(t =>
                t.AppendMeasurementSummaryToFile(fileName, scenarioMeasurements.Key, PerformanceMetricType.VssEnd));
            }
        }

        /// <summary>
        /// save measurements data to file
        /// </summary>
        /// <param name="fileName"></param>
        public static void DumpAllMeasurementsDataToFile(String fileName)
        {
            foreach (var scenarioMeasurements in AllScenarioMeasurements)
            {
                AllScenarioMeasurements[scenarioMeasurements.Key].ForEach(t =>
                t.AppendMeasurementsDataToFile(fileName, scenarioMeasurements.Key));
            }
        }
    }
}
