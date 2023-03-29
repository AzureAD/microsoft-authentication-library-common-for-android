package com.microsoft.identity.common.internal.commands.parameters

import com.microsoft.identity.common.java.util.ObjectMapper

class UserAttributes(val userAttributes: Map<String, String>) {
    companion object Builder {
        private const val GIVEN_NAME = "given_name"
        private const val SURNAME = "surname"
        private const val PHONE_NUMBER = "phone_number"

        private val userAttributes = mutableMapOf<String, String>()

        fun givenNameAttribute(givenName: String): Builder {
            userAttributes[GIVEN_NAME] = givenName
            return this
        }

        fun surnameAttribute(surname: String): Builder {
            userAttributes[SURNAME] = surname
            return this
        }

        fun phoneNumberAttribute(phoneNumber: String): Builder {
            userAttributes[PHONE_NUMBER] = phoneNumber
            return this
        }

        fun customAttribute(key: String, value: String): Builder {
            userAttributes[key] = value
            return this
        }

        fun build(): UserAttributes {
            return UserAttributes(userAttributes)
        }
    }
}

fun UserAttributes.toJsonString(): String {
    val json = ObjectMapper.serializeObjectToJsonString(this.userAttributes)
    return json
}
