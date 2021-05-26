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

using System.IO;
using System.Xml;
using System.Xml.Serialization;

//---------------------------------------------------------------------------
// <summary>
// This File contains different Classes for XML Serialization and Deserialization
// </summary>
// <copyright file="XMLUtility.cs" company="Microsoft">
// Copyright (c) Microsoft Corporation.  All rights reserved.
// </copyright>
// <owner>
// omkrishhn
// </owner>
//-------------------------------------------------------------------------------

namespace PerfClTool
{
    /// <summary>
    /// Class for XML Utility methods
    /// </summary>
    internal class XmlUtility
    {
        /// <summary>
        /// Method to deserialize an XML file to type
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="filePath"></param>
        /// <returns></returns>
        public static T DeSerialize<T>(string filePath)
        {
            T pmConfig;

            Stream stream = null;
            try
            {
                stream = new FileStream(filePath, FileMode.Open, FileAccess.Read);
                using (var xmlReader = new XmlTextReader(stream))
                {
                    stream = null;
                    xmlReader.DtdProcessing = DtdProcessing.Prohibit;
                    var xmlSerializer = new XmlSerializer(typeof(T));
                    pmConfig = (T)xmlSerializer.Deserialize(xmlReader);
                }
            }
            finally
            {
                if (stream != null)
                    stream.Dispose();
            }
            return pmConfig;
        }

        /// <summary>
        /// Serializes an object of Type T to the specified file-path
        /// </summary>
        /// <typeparam name="T"></typeparam>
        /// <param name="obj"></param>
        /// <param name="filePath"></param>
        public static void Serialize<T>(T obj, string filePath)
        {
            var xmlSerializer = new XmlSerializer(obj.GetType());
            using (var writer = new StreamWriter(filePath))
            {
                xmlSerializer.Serialize(writer, obj);
            }
        }
    }
}
