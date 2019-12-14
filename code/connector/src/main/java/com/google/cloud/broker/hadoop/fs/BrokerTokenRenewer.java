// Copyright 2019 Google LLC
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.broker.hadoop.fs;

import java.io.IOException;
import java.security.PrivilegedAction;

import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.security.token.TokenRenewer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.conf.Configuration;

import com.google.cloud.hadoop.fs.gcs.auth.GcsDelegationTokens;

// Classes dynamically generated by protobuf-maven-plugin:
import com.google.cloud.broker.apps.brokerserver.protobuf.RenewSessionTokenRequest;
import com.google.cloud.broker.apps.brokerserver.protobuf.RenewSessionTokenResponse;
import com.google.cloud.broker.apps.brokerserver.protobuf.CancelSessionTokenRequest;
import com.google.cloud.broker.apps.brokerserver.protobuf.CancelSessionTokenResponse;


public class BrokerTokenRenewer extends TokenRenewer {

    @Override
    public boolean handleKind(Text kind) {
        return BrokerTokenIdentifier.KIND.equals(kind);
    }

    @Override
    public long renew(Token<?> t, Configuration conf) throws IOException {
        Token<BrokerTokenIdentifier> token = (Token<BrokerTokenIdentifier>) t;
        BrokerTokenIdentifier tokenIdentifier = (BrokerTokenIdentifier) GcsDelegationTokens.extractIdentifier(token);
        UserGroupInformation loginUser = UserGroupInformation.getLoginUser();
        RenewSessionTokenResponse response = loginUser.doAs((PrivilegedAction<RenewSessionTokenResponse>) () -> {
            BrokerGateway gateway = new BrokerGateway(conf);
            RenewSessionTokenRequest request = RenewSessionTokenRequest.newBuilder()
                .setSessionToken(tokenIdentifier.getSessionToken())
                .build();
            RenewSessionTokenResponse r = gateway.getStub().renewSessionToken(request);
            gateway.getManagedChannel().shutdown();
            return r;
        });
        return response.getExpiresAt();
    }

    @Override
    public void cancel(Token<?> t, Configuration conf) throws IOException {
        Token<BrokerTokenIdentifier> token = (Token<BrokerTokenIdentifier>) t;
        BrokerTokenIdentifier tokenIdentifier = (BrokerTokenIdentifier) GcsDelegationTokens.extractIdentifier(token);
        UserGroupInformation loginUser = UserGroupInformation.getLoginUser();
        loginUser.doAs((PrivilegedAction<CancelSessionTokenResponse>) () -> {
            BrokerGateway gateway = new BrokerGateway(conf);
            CancelSessionTokenRequest request = CancelSessionTokenRequest.newBuilder()
                .setSessionToken(tokenIdentifier.getSessionToken())
                .build();
            CancelSessionTokenResponse response = gateway.getStub().cancelSessionToken(request);
            gateway.getManagedChannel().shutdown();
            return response;
        });
    }

    @Override
    public boolean isManaged(Token<?> token) throws IOException {
        // Return true to indicate that tokens can be renewed and cancelled
        return true;
    }
}