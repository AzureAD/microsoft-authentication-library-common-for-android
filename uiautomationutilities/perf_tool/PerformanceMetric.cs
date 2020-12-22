using System;

namespace PerfClTool.Measurement
{
    public enum PerformanceMetricType
    {
        ResponseTime,
        RssBegin,
        RssEnd,
        RssDelta,
        VssBegin,
        VssEnd,
        VssDelta
    }

    internal abstract class PerformanceMetric
    {
        public abstract String Name { get; }
        public double MeasurementValue { get; set; }
        public PerformanceMetric(PerfDataRecord start, PerfDataRecord end){}
        public PerformanceMetric(Double value) { }
        public override string ToString() => MeasurementValue.ToString(format: "F2");
    }
    internal class ResponseTimeMetric : PerformanceMetric
    {
        public ResponseTimeMetric(double value) : base(value) => MeasurementValue = value;
        public ResponseTimeMetric(PerfDataRecord start, PerfDataRecord end) : base(start, end) => MeasurementValue = end.GetMsResponseTime() - start.GetMsResponseTime();
        public override string Name => "ResponseTime(ms)";
    }

    internal class RssBeginMetric : PerformanceMetric
    {
        public RssBeginMetric(double value) : base(value) => MeasurementValue = value;
        public RssBeginMetric(PerfDataRecord start, PerfDataRecord end) : base(start, end) => MeasurementValue = start.GetKbResidentMemory();
        public override string Name => "RssBegin(KB)";
    }

    internal class RssEndMetric : PerformanceMetric
    {
        public RssEndMetric(double value) : base(value) => MeasurementValue = value;
        public RssEndMetric(PerfDataRecord start, PerfDataRecord end) : base(start, end) => MeasurementValue = end.GetKbResidentMemory();
        public override string Name => "RssEnd(KB)";
    }

    internal class RssDeltaMetric : PerformanceMetric
    {
        public RssDeltaMetric(double value) : base(value) => MeasurementValue = value;
        public RssDeltaMetric(PerfDataRecord start, PerfDataRecord end) : base(start, end) => MeasurementValue = end.GetKbResidentMemory() - start.GetKbResidentMemory();
        public override string Name => "RssDelta(KB)";
    }

    internal class VssBeginMetric : PerformanceMetric
    {
        public VssBeginMetric(double value) : base(value) => MeasurementValue = value;
        public VssBeginMetric(PerfDataRecord start, PerfDataRecord end) : base(start, end) => MeasurementValue = start.GetKbVirtualMemory();
        public override string Name => "VssBegin(KB)";
    }

    internal class VssEndMetric : PerformanceMetric
    {
        public VssEndMetric(double value) : base(value) => MeasurementValue = value;
        public VssEndMetric(PerfDataRecord start, PerfDataRecord end) : base(start, end) => MeasurementValue = end.GetKbVirtualMemory();
        public override string Name => "VssEnd(KB)";
    }

    internal class VssDeltaMetric : PerformanceMetric
    {
        public VssDeltaMetric(double value) : base(value) => MeasurementValue = value;
        public VssDeltaMetric(PerfDataRecord start, PerfDataRecord end) : base(start, end) => MeasurementValue = end.GetKbVirtualMemory() - start.GetKbVirtualMemory();
        public override string Name => "VssDelta(KB)";
    }
}
