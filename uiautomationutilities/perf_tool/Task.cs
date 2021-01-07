
using System.Collections.Generic;

namespace PerfDiffResultMailer
{
    public class Task
    {
        public string Id { get; set; }
        public string Type { get; set; }
        public string AppName { get; set; }
        public string Checkpoint { get; set; }
        public string LogsDir { get; set; }
        public string Device { get; set; }
        public string AndroidVersion { get; set; }
        public string ApkPath { get; set; }
        public Dictionary<string,string> FeatureGateOverrides { get; set; }
        public Dictionary<MeasurementsData,double> Gen3MeasurementDetails { get; set; }
    }
}
