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
