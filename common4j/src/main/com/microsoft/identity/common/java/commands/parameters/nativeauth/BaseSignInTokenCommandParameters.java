package com.microsoft.identity.common.java.commands.parameters.nativeauth;

import com.google.gson.annotations.Expose;
import com.microsoft.identity.common.java.authscheme.AbstractAuthenticationScheme;

import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class BaseSignInTokenCommandParameters extends BaseNativeAuthCommandParameters {
   private static final String TAG = BaseSignInTokenCommandParameters.class.getSimpleName();

   public final List<String> scopes;

   @Expose()
   private final AbstractAuthenticationScheme authenticationScheme;
}