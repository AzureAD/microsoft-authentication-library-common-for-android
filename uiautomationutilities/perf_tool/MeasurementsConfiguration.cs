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
        private static Dictionary<string, MeasurementConfiguration> _measurementConfigurations = new Dictionary<string, MeasurementConfiguration>(StringComparer.OrdinalIgnoreCase);
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
                            //throw new Utils.CustomException<Utils.MarkerConfigurationNotFoundException>($"No measurement details found for measurement ID - {measurement}");
                            throw new Exception();
                        }
                    }
                }
            }
        }

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
            return _appScenarios[appName][scenarioName].Select(t => new PerfMeasurementConfiguration( _measurementConfigurations[t.Id], Convert.ToBoolean(t.IsPrimary))).ToList();
        }
    }
}
