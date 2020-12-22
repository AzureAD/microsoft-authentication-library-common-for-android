using System.Collections.Generic;

namespace PerfDiffResultMailer
{
    public class Parameter
    {
       
        public  string Name { get; set; }
        public string DatabaseColumn { get; set; }
        public  int Threshhold { get; set; }
        public  string Color { get; set; }
        public  string BaseCheckpoint { get; set; }
        public  string TargetCheckPoint { get; set; }
        public  string BaseLogId { get; set; }
        public  string BaseLogDir { get; set; }
        public  string TargetLogId { get; set; }
        public  string TargetLogDir { get; set; }

        public  Dictionary<string,Dictionary<string,double>> BaseScenarioToPerfValueMap { get; set; }
        public  Dictionary<string, Dictionary<string, double>> TargetScenarioToPerfValueMap { get; set; }

        public void SetParameter(Task baseTask, Task targetTask)
        {
            BaseLogId = baseTask.Id;
            TargetLogId = targetTask.Id;
            BaseLogDir = baseTask.LogsDir;
            TargetLogDir = targetTask.LogsDir;
            BaseCheckpoint = baseTask.Checkpoint;
            TargetCheckPoint = targetTask.Checkpoint;
        }

        public Parameter(string name, string column, int threshhold, string color)
        {
            Name = name;
            DatabaseColumn = column;
            Threshhold = threshhold;
            Color = color;
        }
    }
}
