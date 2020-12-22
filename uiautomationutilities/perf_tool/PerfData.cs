using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text;

namespace PerfClTool.Measurement
{
    internal class PerfData
    {
        public List<PerfDataRecord> PerfDataRecordsList = new List<PerfDataRecord>();
        private PerfDataRecord _headers;
        //private static readonly string s_perfMarkersNameMappingFile = Path.Combine("codemarkersMapping.csv");
        public PerfData(string filePath)
        {
            if (!File.Exists(filePath))
            {
                throw new Exception($"File not found {filePath}");
            }
            PerfDataRecordsList = File.ReadLines(filePath)
                .Select(line => line.Split(','))
                .Select(tokens => new PerfDataRecord(
                    tokens[0], //TimeStamp
                    tokens[1], //Marker
                    tokens[2], //Time
                    tokens[3], //Thread
                    tokens[4], //CpuUsed
                    tokens[5], //CpuTotal
                    tokens[6], //ResidentSize
                    tokens[7], //VirtualSize
                    tokens[8], //WifiSent
                    tokens[9], //WifiRecv
                    tokens[10], //WwanSent
                    tokens[11], //WwanRecv
                    tokens[12], //AppSent
                    tokens[13], //AppRecv
                    tokens[14], //Battery
                    tokens[15], //SystemDiskRead
                    tokens[16] //SystemDiskWrite
                    )
                ).ToList();
            //Check if header is present
            if (PerfDataRecordsList.ElementAt(0).TimeStamp.Equals("TimeStamp"))
            {
                _headers = PerfDataRecordsList.ElementAt(0);
                PerfDataRecordsList.RemoveAt(0);
            }
        }
        public PerfDataRecord FindMarker(String marker, int skipCount = 0)
        {
            var records = PerfDataRecordsList.Where(t => t.Marker.Equals(marker, StringComparison.InvariantCultureIgnoreCase));
            if (records.Count() > skipCount)
            {
                return records.ElementAt(skipCount);
            }
            else
            {
                //PerfConsole.LogErrorMessage($"Marker {marker} not found");
                return null;
            }
        }
        public void AddMarkerNames()
        {
            //Dictionary<string, string> markersDict = new Dictionary<string, string>();
            //using (var reader = new StreamReader("C:\\codemarkersMapping.csv"))
            //{
            //    string line;
            //    while ((line = reader.ReadLine()) != null)
            //    {
            //        var parts = line.Split(',');
            //        markersDict.Add(parts[0], parts[1]);
            //    }
            //}

            _headers.MarkerName = "MarkerName";
            /*foreach(var perfdataRecord in PerfDataRecordsList)
            {
                if(markersDict.ContainsKey(perfdataRecord.Marker))
                {
                    perfdataRecord.MarkerName = markersDict[perfdataRecord.Marker];
                }
            }*/
        }
        public void AdjustTimeElapsed()
        {
            var firstMarkerTimeElapsed = Int64.Parse(PerfDataRecordsList.ElementAt(0).Time);
            if (firstMarkerTimeElapsed != 0)
            {
                foreach (var perfRecord in PerfDataRecordsList)
                {
                    perfRecord.Time = (Int64.Parse(perfRecord.Time) - firstMarkerTimeElapsed).ToString();
                }
            }
        }

        public static void AppendAllHeadersToFile(String filePath)
        {
            StringBuilder sb = new StringBuilder();
            //Add Headers
            sb.AppendLine("Marker,MarkerName,Time(ms),Thread,ResidentSize,VirtualSize");
            File.AppendAllText(filePath, sb.ToString());
        }

        public void AppendMarkersDataToFile(string filePath)
        {
            var sb = new StringBuilder();
            foreach(var dataRecord in PerfDataRecordsList)
            {
                sb.AppendLine($"{dataRecord.Marker},{dataRecord.MarkerName},{long.Parse(dataRecord.Time)/1000},{dataRecord.Thread},{dataRecord.ResidentSize},{dataRecord.VirtualSize}");
            }
            File.AppendAllText(filePath, sb.ToString());
        }

        public void AddPidCreationTime(DateTime startTime)
        {
            var firstRecord = PerfDataRecordsList.ElementAt(0);
            if (!(firstRecord.Marker.Equals("1") || firstRecord.Marker.Equals("10011")))
            {
                throw new Exception("Unable to find first marker in PerfData.txt");
            }
            var timeDifference = DateTime.Parse(firstRecord.TimeStamp) - startTime;

            var logcatRecord = new PerfDataRecord()
            {
                TimeStamp = startTime.ToString("yyyy-MM-ddTHH:mm:ss.fff"),
                Marker = "logcat",
                Time = Convert.ToString(Convert.ToInt64(firstRecord.Time) - timeDifference.TotalMilliseconds * 1000, CultureInfo.InvariantCulture),
                Thread = firstRecord.Thread,
                CpuUsed = firstRecord.CpuUsed.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.CpuUsed : "0",
                CpuTotal = firstRecord.CpuTotal.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.CpuTotal : "0",
                ResidentSize = firstRecord.ResidentSize.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.ResidentSize : "0",
                VirtualSize = firstRecord.VirtualSize.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.VirtualSize : "0",
                WifiSent = firstRecord.WifiSent.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WifiSent : "0",
                WifiRecv = firstRecord.WifiRecv.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WifiRecv : "0",
                WwanSent = firstRecord.WwanSent.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WwanSent : "0",
                WwanRecv = firstRecord.WwanRecv.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WwanRecv : "0",
                AppSent = firstRecord.AppSent.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.AppSent : "0",
                AppRecv = firstRecord.AppRecv.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.AppRecv : "0",
                Battery = firstRecord.Battery.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.Battery : "0",
                SystemDiskRead = firstRecord.SystemDiskRead.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.SystemDiskRead : "0",
                SystemDiskWrite = firstRecord.SystemDiskWrite.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.SystemDiskWrite : "0"
            };
            PerfDataRecordsList.Insert(0, logcatRecord);
        }

        public void AddActivityDisplayTime(long activityDisplayTimeMs)
        {
            var firstRecord = PerfDataRecordsList.ElementAt(0);
            var logcatRecord = new PerfDataRecord()
            {
                TimeStamp = DateTime.Parse(firstRecord.TimeStamp).AddMilliseconds(activityDisplayTimeMs).ToString("yyyy-MM-ddTHH:mm:ss.fff"),
                Marker = "activityDisplay",
                Time = Convert.ToString(Convert.ToInt64(firstRecord.Time) + activityDisplayTimeMs* 1000, CultureInfo.InvariantCulture),
                Thread = firstRecord.Thread,
                CpuUsed = firstRecord.CpuUsed.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.CpuUsed : "0",
                CpuTotal = firstRecord.CpuTotal.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.CpuTotal : "0",
                ResidentSize = firstRecord.ResidentSize.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.ResidentSize : "0",
                VirtualSize = firstRecord.VirtualSize.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.VirtualSize : "0",
                WifiSent = firstRecord.WifiSent.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WifiSent : "0",
                WifiRecv = firstRecord.WifiRecv.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WifiRecv : "0",
                WwanSent = firstRecord.WwanSent.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WwanSent : "0",
                WwanRecv = firstRecord.WwanRecv.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.WwanRecv : "0",
                AppSent = firstRecord.AppSent.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.AppSent : "0",
                AppRecv = firstRecord.AppRecv.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.AppRecv : "0",
                Battery = firstRecord.Battery.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.Battery : "0",
                SystemDiskRead = firstRecord.SystemDiskRead.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.SystemDiskRead : "0",
                SystemDiskWrite = firstRecord.SystemDiskWrite.Equals(PerfDataRecord.ValueNotApplicable) ? firstRecord.SystemDiskWrite : "0"
            };
            PerfDataRecordsList.Insert(1, logcatRecord);
        }
    }
}
