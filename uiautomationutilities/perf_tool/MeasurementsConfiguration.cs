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
using System.IO;
using System.Linq;
namespace PerfClTool.Measurement

{
    using AppScenarios = Dictionary<string, List<Measurement>>;
    using MeasurementData = Dictionary<string, Dictionary<string, List<Measurement>>>;

    internal class PerfMeasurementConfiguration : MeasurementConfiguration
    {
        public bool IsPrimary { get; set; }
        public PerfMeasurementConfiguration(MeasurementConfiguration measurementConfiguration, bool isPrimary)
        {
            EndMarker = measurementConfiguration.EndMarker;
            EndSkip = measurementConfiguration.EndSkip;
            Id = measurementConfiguration.Id;
            Name = measurementConfiguration.Name;
            IsPrimary = isPrimary;
            StartMarker = measurementConfiguration.StartMarker;
            Startskip = measurementConfiguration.Startskip;
        }
    }

    internal class MeasurementsConfiguration
    {
        private static readonly string configurationFile = "PerfDataConfiguration.xml";
        private static MeasurementData _appScenarios = new MeasurementData(StringComparer.OrdinalIgnoreCase);
        private static Dictionary<string, MeasurementConfiguration> _measurementConfigurations = 
            new Dictionary<string, MeasurementConfiguration>(StringComparer.OrdinalIgnoreCase);
        private static List<string> AllScenarios = new List<string>();

        public static List<string> getAllScenarioNames()
        {
            return AllScenarios;
        }

        static MeasurementsConfiguration()
        {
            var myConfiguration = XmlUtility.DeSerialize<PerfDataConfiguration>(configurationFile);
            
            //Read all measurement configurations
            var measurementConfigurations = myConfiguration.MeasurementsConfigurations.MeasurementConfiguration;
            foreach(var measurement in measurementConfigurations)
            {
                _measurementConfigurations.Add(measurement.Id, measurement);
            }

            //Read all the measurements for apps/scenarios
            var apps = myConfiguration.ScenarioMeasurementsMapping.Apps.App;
            foreach(var app in apps)
            {
                var appName = app.Name;
                _appScenarios[appName]  = new AppScenarios(StringComparer.OrdinalIgnoreCase);
                var scenarios = app.Scenarios.Scenario;
                foreach(var scenario in scenarios)
                {
                    string scenarioName = scenario.Name;
                    AllScenarios.Add(scenarioName);
                    _appScenarios[appName][scenarioName] = scenario.Measurements.Measurement;
                    
                    //Assert that for each measurement Id defined for a scenario, measurement details exist
                    foreach (var measurement in _appScenarios[appName][scenarioName])
                    {
                        if(!_measurementConfigurations.ContainsKey(measurement.Id))
                        {
                            throw new Exception();
                        }
                    }
                }
            }
        }

        // get configuration given an app name and a scenario
        internal static List<PerfMeasurementConfiguration> GetMeasurementConfigurations(String appName, String scenarioName)
        {
            if(!_appScenarios.ContainsKey(appName))
            {
                throw new Exception($"no configuration data found for app {appName}");
            }
            if(!_appScenarios[appName].ContainsKey(scenarioName))
            {
                throw new Exception($"no configuration data found for app {appName}, scenario {scenarioName}");
            }
            return _appScenarios[appName][scenarioName].Select(t => new PerfMeasurementConfiguration(
                _measurementConfigurations[t.Id], Convert.ToBoolean(t.IsPrimary))).ToList();
        }
    }
}
