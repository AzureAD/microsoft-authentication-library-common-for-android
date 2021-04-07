package com.microsoft.identity.common.internal.commands.parameters;

import lombok.Getter;
import lombok.Value;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@Value
public class EnvelopeCommandParameters extends CommandParameters {

    /**
     * The name of the command to execute.
     */
    private String commandName;
    /**
     * The type of the data contained in the command array.
     */
    private String type;
    /**
     * The command information to execute
     */
    private byte[] command;
}
