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
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace PerfDiffResultMailer
{
    // This class's object defines a particular measurement of a particular scenario. 
    // This works as a key to refer to the measurement and the value of the particular measurement is kept in the Disctionary in class Task.
    public class MeasurementsData : IEquatable<MeasurementsData>
    {
        public string ScenarioName { get; set; }
        public string ActivityName { get; set; }
        public string MeasurementName { get; set; }

        public override bool Equals(object obj)
        {
            return this.Equals(obj as MeasurementsData);
        }
        public override int GetHashCode()
        {
            string x = this.MeasurementName + this.ScenarioName + this.ActivityName;
            return x.GetHashCode();
        }
        public bool Equals(MeasurementsData other)
        {
            if (other == null)
                return false;

            return (this.ScenarioName.Equals(other.ScenarioName)) &&
                (this.ActivityName.Equals(other.ActivityName)) && (this.MeasurementName.Equals(other.MeasurementName));                
        }
    }
}
