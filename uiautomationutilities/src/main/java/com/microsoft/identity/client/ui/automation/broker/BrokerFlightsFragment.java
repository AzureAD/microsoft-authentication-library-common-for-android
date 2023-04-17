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
package com.microsoft.identity.client.ui.automation.broker;

import androidx.annotation.NonNull;

public class BrokerFlightsFragment extends AbstractBrokerHost {
    // Resource Id for the buttons
    private static final String OVERVIEW_LOCAL_FLIGHTS_BUTTON_ID = "button_overwrite_flights";
    private static final String GET_FLIGHTS_BUTTON_ID = "button_get_flights";
    private static final String SET_LOCAL_FLIGHT_BUTTON_ID = "button_set_flights";
    private static final String CLEAR_LOCAL_FLIGHTS_BUTTON_ID = "button_clear_flights";
    // Resource Id for the edit text
    private static final String KEY_FLIGHT_EDIT_TEXT_ID = "edit_text_flight_key";
    private static final String VALUE_FLIGHT_EDIT_TEXT_ID = "edit_text_flight_value";
    private static final String FLIGHTS_EDIT_TEXT_ID = "edit_text_flights";

    public void setLocalFlight(@NonNull final String key, @NonNull final String value) {
        fillTextBox(KEY_FLIGHT_EDIT_TEXT_ID, key);
        fillTextBox(VALUE_FLIGHT_EDIT_TEXT_ID, value);
        clickButton(SET_LOCAL_FLIGHT_BUTTON_ID);
    }

    public void overWriteLocalFlights(@NonNull final String flights) {
        fillTextBox(FLIGHTS_EDIT_TEXT_ID, flights);
        clickButton(OVERVIEW_LOCAL_FLIGHTS_BUTTON_ID);
        dismissDialogBoxAndAssertContainsText("Flight set in broker host.");
    }

    public String getFlights() {
        clickButton(GET_FLIGHTS_BUTTON_ID);
        return readTextBox(FLIGHTS_EDIT_TEXT_ID);
    }

    @Override
    public void launch() {
        launch(BrokerHostNavigationMenuItem.FLIGHTS_API);
    }
}
