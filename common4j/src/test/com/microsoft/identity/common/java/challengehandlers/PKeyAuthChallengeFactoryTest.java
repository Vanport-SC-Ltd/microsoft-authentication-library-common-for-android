// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.java.challengehandlers;

import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.PKEYAUTH_VERSION;
import static com.microsoft.identity.common.java.exception.ErrorStrings.DEVICE_CERTIFICATE_REQUEST_INVALID;

import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

public class PKeyAuthChallengeFactoryTest {

    private final String[] CERT_AUTHORITIES = new String[]{
            "OU=82dbaca4-3e81-46ca-9c73-0950c1eaca97,CN=MS-Organization-Access,DC=windows,DC=net"
    };
    private final String PKEYAUTH_AUTH_ENDPOINT_CONTEXT = "rQIIAa2RvW_TQBjGfXGSNqGFlImpytBKFegcn78dCYmmKZB-JAqFfi3obN8lVzl24kubNjMCxIQ6MgESA5mgYkAMiKlDB5QRwYZgQQwdI4EEKVX_A57h9z7TM_zedAJJumRfFhVJzk9pjuw5lqtAmyoG1DRkQsvxCFSpTFRDN6hF5OhiOjP_-3Dy-9JIef94vPIiR7_0QKXB8Xa7ns_l3LAhNThtS8wjQZu19yTXZ8Mmcdxo-kTyQxf7OdQptdZXWoUtszOtFOoIbTLKg1antrcYbUyrxbcA9AHYjwn9GPgcu1CZHY4rJwgj1iVPxLKiWVZeUW0TIw1DKnsIajpSoYVtAyqGRXXbVi1H16FsI4u6JoIKdUyoWbYNMbJ0iC0FuzKl1FZRT5yST6PCE57CPWtnORDHw6iGA9bFbRYG_Eic2eYkkiKCvWw2bJKAedmQUp8F5C52XcJ5thmFlPnkkwi-ipPM87FzrcH_XU0KgwZzo5CHQ11Db_04-BEfGwWZxKVUVpiZkMEgDp4nhsIn3qNvv-69mTt48HTjI6fCUSK3q1d3iwurZVIv-o621aTImMdygemVBWPZWa0u8PWdkotcs1W9aubR4yQ4SMZHhQw4So4tr8wuSbOBF4XMO07GHo4I71L_-4ODlNBLX1nb4V7neql2e_fWjRbVUbdwZ66EFpVFo9Je3URRszVXM9aUctCtvkqD_jnwYVwYnH_26PD1_Zd_ft78Cw2.AQABAAEAAAD--DLA3VO7QrddgJg7WevrS-SEPcQWHvmlxD6gDZD4tzKpufY8FnfGkxc4ZD_1S0UqyCs1aOsjmFmxdz8JIxakFka_Q5aaf-RlurMM4UAp9qjBryW6lR2_GJCMvauswa2GVowfL7H099Oo2j1EBe0ddzAZvImhXh7Q-fIXWNfA20OTMPSdKHPWRlgJJrxqG2EW5XcXwrUiCQ3Zw3vbDtULiibI8DvWfH2AbYachYABPaBHQu64twJXtOIZRUPMd5cQvFtNzBHBE_x907m9bB4F_kPeYkMZTXr42CCGDToVnsPwcx_dd2Zm7yxeBrN6PspSq_FysaHy0yJ1dYh3kOT2AjhEwvm9vLh2uPq2wbeJbHZ-sWvaSAeJpON8Q-AxNCxB8dTiAv96kQfDkryshQabQsb86CjIHY26oeBuXFZ4pNy6jZEEXdJ7z2pD5iuZFH9zpyQTQGuq6esy1nUKRA_feNtqBqIVo6HwRgAzmz1COwRdY7B_8NEy8XbWLercIn-d9o9PCVARSSDGs4e8AhZMV1w-a6TSNlW7D12d6FzwhwG5lRbxjeq1Xb3RuZ67OQylNYT0nEmqhSeiG3xuRCtFnaffk15lFmUSUGV-Ki98CGslIP7IwXQKOOwR9RQlaZl0e-Q-_VnPtl-2E5vX0eh1odBRayxnuCufRTuDT5ra1cvU5YbuLWTGxW2oNaQGMZuhH0r84bpmiULMfzFdh1uYSoQXVNEW5jJrw5FwF_pAD54sLgqGGKGkb4NEkjKV-R8gAA";
    private final String PKEYAUTH_AUTH_ENDPOINT_NONCE = "MV50ggQuxKocqa9BV1Wzp8Gs16sfijXXlfpZJgYEpP4";
    private final String PKEYAUTH_AUTH_ENDPOINT_SUBMIT_URL = "https://login.microsoftonline.com/common/DeviceAuthPKeyAuth";
    private final String PKEYAUTH_AUTH_ENDPOINT_URL = "urn:http-auth:PKeyAuth?CertAuthorities=OU%3d82dbaca4-3e81-46ca-9c73-0950c1eaca97%2cCN%3dMS-Organization-Access%2cDC%3dwindows%2cDC%3dnet&Version=1.0&Context=rQIIAa2RvW_TQBjGfXGSNqGFlImpytBKFegcn78dCYmmKZB-JAqFfi3obN8lVzl24kubNjMCxIQ6MgESA5mgYkAMiKlDB5QRwYZgQQwdI4EEKVX_A57h9z7TM_zedAJJumRfFhVJzk9pjuw5lqtAmyoG1DRkQsvxCFSpTFRDN6hF5OhiOjP_-3Dy-9JIef94vPIiR7_0QKXB8Xa7ns_l3LAhNThtS8wjQZu19yTXZ8Mmcdxo-kTyQxf7OdQptdZXWoUtszOtFOoIbTLKg1antrcYbUyrxbcA9AHYjwn9GPgcu1CZHY4rJwgj1iVPxLKiWVZeUW0TIw1DKnsIajpSoYVtAyqGRXXbVi1H16FsI4u6JoIKdUyoWbYNMbJ0iC0FuzKl1FZRT5yST6PCE57CPWtnORDHw6iGA9bFbRYG_Eic2eYkkiKCvWw2bJKAedmQUp8F5C52XcJ5thmFlPnkkwi-ipPM87FzrcH_XU0KgwZzo5CHQ11Db_04-BEfGwWZxKVUVpiZkMEgDp4nhsIn3qNvv-69mTt48HTjI6fCUSK3q1d3iwurZVIv-o621aTImMdygemVBWPZWa0u8PWdkotcs1W9aubR4yQ4SMZHhQw4So4tr8wuSbOBF4XMO07GHo4I71L_-4ODlNBLX1nb4V7neql2e_fWjRbVUbdwZ66EFpVFo9Je3URRszVXM9aUctCtvkqD_jnwYVwYnH_26PD1_Zd_ft78Cw2.AQABAAEAAAD--DLA3VO7QrddgJg7WevrS-SEPcQWHvmlxD6gDZD4tzKpufY8FnfGkxc4ZD_1S0UqyCs1aOsjmFmxdz8JIxakFka_Q5aaf-RlurMM4UAp9qjBryW6lR2_GJCMvauswa2GVowfL7H099Oo2j1EBe0ddzAZvImhXh7Q-fIXWNfA20OTMPSdKHPWRlgJJrxqG2EW5XcXwrUiCQ3Zw3vbDtULiibI8DvWfH2AbYachYABPaBHQu64twJXtOIZRUPMd5cQvFtNzBHBE_x907m9bB4F_kPeYkMZTXr42CCGDToVnsPwcx_dd2Zm7yxeBrN6PspSq_FysaHy0yJ1dYh3kOT2AjhEwvm9vLh2uPq2wbeJbHZ-sWvaSAeJpON8Q-AxNCxB8dTiAv96kQfDkryshQabQsb86CjIHY26oeBuXFZ4pNy6jZEEXdJ7z2pD5iuZFH9zpyQTQGuq6esy1nUKRA_feNtqBqIVo6HwRgAzmz1COwRdY7B_8NEy8XbWLercIn-d9o9PCVARSSDGs4e8AhZMV1w-a6TSNlW7D12d6FzwhwG5lRbxjeq1Xb3RuZ67OQylNYT0nEmqhSeiG3xuRCtFnaffk15lFmUSUGV-Ki98CGslIP7IwXQKOOwR9RQlaZl0e-Q-_VnPtl-2E5vX0eh1odBRayxnuCufRTuDT5ra1cvU5YbuLWTGxW2oNaQGMZuhH0r84bpmiULMfzFdh1uYSoQXVNEW5jJrw5FwF_pAD54sLgqGGKGkb4NEkjKV-R8gAA&nonce=MV50ggQuxKocqa9BV1Wzp8Gs16sfijXXlfpZJgYEpP4&SubmitUrl=https%3a%2f%2flogin.microsoftonline.com%2fcommon%2fDeviceAuthPKeyAuth";

    private final String PKEYAUTH_TOKEN_ENDPOINT_CONTEXT = "rQIIAeNiNdQz1bPUYjbSM7BSMUkySEmySDbStUwzMtM1MTE017VISknVNU4zSDU2MzVLs0g1KBLiEjjb_F946uHJbnNXzk0IdlaqfMHIeIGJ8RYTt79jaUmGUUh-dmreKmYVAwgw1gWRECIZxoKBTcwqaWYmpokplka6qcYWKbomKYaJukmmhga6KYZJBpaJ5iaJFsmJp5jVS4tTi_SKUhNTFPILUvMygVRaWk5mXmp8YnJyanGxQkFRflpmTuoEFsYLLCyvWMQ4GAVYJTgVGDQEDRi9ODhYBBgkGBQYFrEC3X_ObG2x7Ul_31bX6eesLnYxTGCTmcDGMYGNbQKb8CY2Fg4GAcZTbDy-wY4-eo55KUX5mSkf2Bg72Bl-cDLM4GK8wM14gJfhB9-c81N61q_9_8YDAA2.AQABAAEAAAD--DLA3VO7QrddgJg7WevritOStff29rmP7pxoNaAajotJtvq_lSIqVLdNZgszjRcnTW3gQPrpYxT6hydXcUMEuASqhFvxXPBs0JVggjbyO9s9oDTZQqRsoz9w1cQPpcagNBBUhdyw89DU4XvM4HWIQ_pdn6FcUX0ixrkCyNR6e3U-1TNP3ctH_l5kq--9IWJ8MIs4rrchcCSu_A7b66fOwyVFTJnQhnpTLd9d5crH1lAlYOuATsSKAu7q2oR5UNEMMqJS8gemwdxl2NtyA0yh4cyaxWOeeEdEVoRAehKCe0xFVgsfHoUOdf6WLHwWnWCh1ptV1xdaYVyLFCd1TFbUoFrM_wNWzfFo9vOT1Vy_JSSkkLmn3tZEYw4tCPKnG04b-qJPciDSVg2m9DIfZCYreQxgPCDAwjFhnIOmPz_-TP4dpS2bP-HVev0C1-l3t00EyNQDs4ZdbWwyL_zY_zWglsVwoedl3eF2w0DhAys2NFXEj4D6Nf5EoEQES3yDeSpDdZHyMQNxJoVRiN1SSqqrjUr1pwbExDjzJ1p19xvcQRJDn2iNdWAlxvRWrQC3D-3JPY4cRKYaip-HQ-HrHRGIC2V_ry36yGS73e0pEoCSKMaebO-6Jyiqmiy1fMzePRwTZzDeyBPBw3eYfMW-6H_KIAA";
    private final String PKEYAUTH_TOKEN_ENDPOINT_NONCE = "ZS6jQlJQLnntAXLP2wMtDl9QvhnNyidmKuaC63rz_pA";
    private final String PKEYAUTH_TOKEN_ENDPOINT_AUTHORITY = "https://login.microsoftonline.com/f645ad92-e38d-4d1a-b510-d1b09a74a8ca/oAuth2/v2.0/token";
    private final String PKEYAUTH_TOKEN_ENDPOINT_CHALLENGE_HEADER = "PKeyAuth CertAuthorities=\"OU=82dbaca4-3e81-46ca-9c73-0950c1eaca97,CN=MS-Organization-Access,DC=windows,DC=net\", Version=\"1.0\", Context=\"rQIIAeNiNdQz1bPUYjbSM7BSMUkySEmySDbStUwzMtM1MTE017VISknVNU4zSDU2MzVLs0g1KBLiEjjb_F946uHJbnNXzk0IdlaqfMHIeIGJ8RYTt79jaUmGUUh-dmreKmYVAwgw1gWRECIZxoKBTcwqaWYmpokplka6qcYWKbomKYaJukmmhga6KYZJBpaJ5iaJFsmJp5jVS4tTi_SKUhNTFPILUvMygVRaWk5mXmp8YnJyanGxQkFRflpmTuoEFsYLLCyvWMQ4GAVYJTgVGDQEDRi9ODhYBBgkGBQYFrEC3X_ObG2x7Ul_31bX6eesLnYxTGCTmcDGMYGNbQKb8CY2Fg4GAcZTbDy-wY4-eo55KUX5mSkf2Bg72Bl-cDLM4GK8wM14gJfhB9-c81N61q_9_8YDAA2.AQABAAEAAAD--DLA3VO7QrddgJg7WevritOStff29rmP7pxoNaAajotJtvq_lSIqVLdNZgszjRcnTW3gQPrpYxT6hydXcUMEuASqhFvxXPBs0JVggjbyO9s9oDTZQqRsoz9w1cQPpcagNBBUhdyw89DU4XvM4HWIQ_pdn6FcUX0ixrkCyNR6e3U-1TNP3ctH_l5kq--9IWJ8MIs4rrchcCSu_A7b66fOwyVFTJnQhnpTLd9d5crH1lAlYOuATsSKAu7q2oR5UNEMMqJS8gemwdxl2NtyA0yh4cyaxWOeeEdEVoRAehKCe0xFVgsfHoUOdf6WLHwWnWCh1ptV1xdaYVyLFCd1TFbUoFrM_wNWzfFo9vOT1Vy_JSSkkLmn3tZEYw4tCPKnG04b-qJPciDSVg2m9DIfZCYreQxgPCDAwjFhnIOmPz_-TP4dpS2bP-HVev0C1-l3t00EyNQDs4ZdbWwyL_zY_zWglsVwoedl3eF2w0DhAys2NFXEj4D6Nf5EoEQES3yDeSpDdZHyMQNxJoVRiN1SSqqrjUr1pwbExDjzJ1p19xvcQRJDn2iNdWAlxvRWrQC3D-3JPY4cRKYaip-HQ-HrHRGIC2V_ry36yGS73e0pEoCSKMaebO-6Jyiqmiy1fMzePRwTZzDeyBPBw3eYfMW-6H_KIAA\", nonce=\"ZS6jQlJQLnntAXLP2wMtDl9QvhnNyidmKuaC63rz_pA\"";

    @Test
    public void testParsingChallengeUrl() throws ClientException {
        final PKeyAuthChallenge challenge = new PKeyAuthChallengeFactory().getPKeyAuthChallengeFromWebViewRedirect(PKEYAUTH_AUTH_ENDPOINT_URL);
        Assert.assertArrayEquals(CERT_AUTHORITIES, challenge.getCertAuthorities().toArray());
        Assert.assertEquals(PKEYAUTH_VERSION, challenge.getVersion());
        Assert.assertEquals(PKEYAUTH_AUTH_ENDPOINT_CONTEXT, challenge.getContext());
        Assert.assertEquals(PKEYAUTH_AUTH_ENDPOINT_NONCE, challenge.getNonce());
        Assert.assertEquals(PKEYAUTH_AUTH_ENDPOINT_SUBMIT_URL, challenge.getSubmitUrl());
        Assert.assertNull(challenge.getThumbprint());
    }

    @Test
    public void testParsingChallengeUrl_Malformed() {
        try{
            new PKeyAuthChallengeFactory().getPKeyAuthChallengeFromWebViewRedirect(
                    "urn:http-auth:PKeyAuth?CertAuthorities=OU%3d82dbaca4"
            );
            Assert.fail("Exception is expected");
        } catch (final ClientException e) {
            Assert.assertEquals(DEVICE_CERTIFICATE_REQUEST_INVALID, e.getErrorCode());
        }
    }

    @Test
    public void testParsingChallengeHeader() throws UnsupportedEncodingException, ClientException {
        final PKeyAuthChallenge challenge = new PKeyAuthChallengeFactory().getPKeyAuthChallengeFromTokenEndpointResponse(
                PKEYAUTH_TOKEN_ENDPOINT_CHALLENGE_HEADER,
                PKEYAUTH_TOKEN_ENDPOINT_AUTHORITY);
        Assert.assertArrayEquals(CERT_AUTHORITIES, challenge.getCertAuthorities().toArray());
        Assert.assertEquals(PKEYAUTH_VERSION, challenge.getVersion());
        Assert.assertEquals(PKEYAUTH_TOKEN_ENDPOINT_CONTEXT, challenge.getContext());
        Assert.assertEquals(PKEYAUTH_TOKEN_ENDPOINT_NONCE, challenge.getNonce());
        Assert.assertEquals(PKEYAUTH_TOKEN_ENDPOINT_AUTHORITY, challenge.getSubmitUrl());
        Assert.assertNull(challenge.getThumbprint());
    }

    @Test
    public void testParsingChallengeHeader_Malformed() throws UnsupportedEncodingException {
        try{
            new PKeyAuthChallengeFactory().getPKeyAuthChallengeFromTokenEndpointResponse(
                    "urn:http-auth:PKeyAuth?CertAuthorities=OU%3d82dbaca4",
                    PKEYAUTH_TOKEN_ENDPOINT_AUTHORITY
            );
            Assert.fail("Exception is expected");
        } catch (final ClientException e) {
            Assert.assertEquals(DEVICE_CERTIFICATE_REQUEST_INVALID, e.getErrorCode());
        }
    }
}
