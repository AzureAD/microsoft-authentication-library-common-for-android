using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;

namespace PerfClTool.Measurement
{
    internal class PerfMeasurementsSet
    {
        public PerfMeasurementConfiguration _measurementConfiguration { get; private set; }
        public List<IterationMeasurement> _iterationMeasurements { get; set; }
        public NumIterations _numIterations { get; private set; }
        public Average _average { get; private set; }
        public Min _min { get; private set; }
        public Max _max { get; private set; }
        public Stdev _stdev { get; private set; }
        public Percentile25 _percentile25 { get; private set; }
        public Percentile50 _percentile50 { get; private set; }
        public Percentile75 _percentile75 { get; private set; }
        public Best75Avg _best75Avg { get; private set; }
        public Best75Stdev _best75Stdev { get; private set; }

        public PerfMeasurementsSet(PerfMeasurementConfiguration measurementConfiguration)
        {
            _measurementConfiguration = measurementConfiguration;
            _iterationMeasurements = new List<IterationMeasurement>();
        }
        public void AddIterationMeasurement(PerfData perfData)
        {
            var startRecord = perfData.FindMarker(_measurementConfiguration.StartMarker, Int32.Parse(_measurementConfiguration.Startskip));
            var endRecord = perfData.FindMarker(_measurementConfiguration.EndMarker, Int32.Parse(_measurementConfiguration.EndSkip));
            if (startRecord == null || endRecord == null)
            {
                if (_measurementConfiguration.IsPrimary)
                {
                    throw new Exception($"markers not found {_measurementConfiguration.StartMarker}, {_measurementConfiguration.EndMarker}");
                }
                else
                {
                    //PerfConsole.LogErrorMessage($"markers not found {_measurementConfiguration.StartMarker}, {_measurementConfiguration.EndMarker}");
                }
            }
            else
            {
                var iterationData = new IterationMeasurement(startRecord, endRecord);

                //Report Max Resident Size/Virtual Size instead of RSS/VSS at a particular moment
                iterationData.RssEnd = new RssEndMetric(perfData.PerfDataRecordsList.Select(t => double.Parse("0.0") / 1000).Max());
                iterationData.VssEnd = new VssEndMetric(perfData.PerfDataRecordsList.Select(t => double.Parse("0.0") / 1000).Max());
                _iterationMeasurements.Add(iterationData);
            }
        }

        public void GenerateAggregateMeasurements()
        {
            _numIterations = new NumIterations(_iterationMeasurements);
            _average = new Average(_iterationMeasurements);
            _min = new Min(_iterationMeasurements);
            _max = new Max(_iterationMeasurements);
            _stdev = new Stdev(_iterationMeasurements);
            _percentile25 = new Percentile25(_iterationMeasurements);
            _percentile50 = new Percentile50(_iterationMeasurements);
            _percentile75 = new Percentile75(_iterationMeasurements);
            _best75Avg = new Best75Avg(_iterationMeasurements);
            _best75Stdev = new Best75Stdev(_iterationMeasurements);
        }

        public static void AppendMeasurementSummaryHeadersToFile(String fileName)
        {
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("Scenario,Measurement,Iterations,Average,StDev,Min,Max,25Percentile,50Percentile,75Percentile,75BestAvg,75BestStdev,IsPrimaryMeasurement");
            File.AppendAllText(fileName, sb.ToString());
        }

        public void AppendMeasurementSummaryToFile(String fileName, String scenarioName, PerformanceMetricType metricType)
        {
            StringBuilder sb = new StringBuilder();
            sb.Append(scenarioName);
            sb.Append("," + _measurementConfiguration.Name);
            foreach (var aggregateMeasurement in new List<AggregatedMeasurement> { _numIterations, _average, _stdev, _min, _max, _percentile25, _percentile50, _percentile75, _best75Avg, _best75Stdev })
            {
                switch (metricType)
                {
                    case PerformanceMetricType.ResponseTime:
                        sb.Append("," + aggregateMeasurement.ResponseTime);
                        break;
                    case PerformanceMetricType.RssBegin:
                        sb.Append("," + aggregateMeasurement.RssBegin);
                        break;
                    case PerformanceMetricType.RssEnd:
                        sb.Append("," + aggregateMeasurement.RssEnd);
                        break;
                    case PerformanceMetricType.RssDelta:
                        sb.Append("," + aggregateMeasurement.RssDelta);
                        break;
                    case PerformanceMetricType.VssBegin:
                        sb.Append("," + aggregateMeasurement.VssBegin);
                        break;
                    case PerformanceMetricType.VssEnd:
                        sb.Append("," + aggregateMeasurement.VssEnd);
                        break;
                    case PerformanceMetricType.VssDelta:
                        sb.Append("," + aggregateMeasurement.VssDelta);
                        break;
                    default:
                        throw new Exception($"Unsupported Performance metric type {metricType}");
                }
            }
            sb.AppendLine("," + _measurementConfiguration.IsPrimary.ToString());
            File.AppendAllText(fileName, sb.ToString());
        }

        public void AppendMeasurementsDataToFile(String fileName, String scenarioName)
        {
            StringBuilder sb = new StringBuilder();

            //Append Headers
            sb.AppendLine("ScenarioName,MeasurementName,ResponseTime(ms),RssBegin(KB),RssEnd(KB),RssDelta(KB),VssBegin(KB),VssEnd(KB),VssDelta(KB),Comments");

            //Append Iterations Data
            var i = 1;
            foreach (var iterationMeasurement in _iterationMeasurements)
            {
                sb.Append(scenarioName);
                sb.Append("," + _measurementConfiguration.Name);
                sb.Append("," + iterationMeasurement.ResponseTime);
                sb.Append("," + iterationMeasurement.RssBegin);
                sb.Append("," + iterationMeasurement.RssEnd);
                sb.Append("," + iterationMeasurement.RssDelta);
                sb.Append("," + iterationMeasurement.VssBegin);
                sb.Append("," + iterationMeasurement.VssEnd);
                sb.Append("," + iterationMeasurement.VssDelta);
                sb.AppendLine($",iteration{i++}");
            }

            //Append Aggregated Data
            foreach (AggregatedMeasurement aggregateData in new List<AggregatedMeasurement> { _numIterations, _average, _min, _max, _stdev, _percentile25, _percentile50, _percentile75, _best75Avg, _best75Stdev })
            {
                sb.Append(scenarioName);
                sb.Append("," + _measurementConfiguration.Name);
                sb.Append("," + aggregateData.ResponseTime);
                sb.Append("," + aggregateData.RssBegin);
                sb.Append("," + aggregateData.RssEnd);
                sb.Append("," + aggregateData.RssDelta);
                sb.Append("," + aggregateData.VssBegin);
                sb.Append("," + aggregateData.RssEnd);
                sb.Append("," + aggregateData.VssDelta);
                sb.AppendLine("," + aggregateData.Name);
            }

            //Add a blank line
            sb.AppendLine();
            File.AppendAllText(fileName, sb.ToString());
        }
    }
}
