/*
 * Copyright 2023 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.bcdns.impl.bif;

import java.util.Objects;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alipay.antchain.bridge.bcdns.impl.bif.req.VcApplyReqDto;
import com.alipay.antchain.bridge.bcdns.impl.bif.resp.DataResp;
import com.alipay.antchain.bridge.bcdns.impl.bif.resp.VcApplyRespDto;
import com.alipay.antchain.bridge.commons.bcdns.AbstractCrossChainCertificate;
import okhttp3.*;

public class BifCertificationServiceClient {

    public static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private static final String VC_APPLY_URL = "/vc/apply";

    private final OkHttpClient httpClient;

    private final String serviceUrl;

    private final BifBCDNSClientCredential clientCredential;

    public BifCertificationServiceClient(String serviceUrl, BifBCDNSClientCredential bifBCDNSClientCredential) {
        if (serviceUrl.startsWith("https://")) {
            try {
                TrustManager[] trustAllCerts = buildTrustManagers();
                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                httpClient = new OkHttpClient.Builder()
                        .sslSocketFactory(
                                sslContext.getSocketFactory(),
                                (X509TrustManager) trustAllCerts[0]
                        ).hostnameVerifier((hostname, session) -> true)
                        .build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            httpClient = new OkHttpClient.Builder().build();
            if (!serviceUrl.startsWith("http://")) {
                serviceUrl = StrUtil.format("http://{}", serviceUrl);
            }
        }

        this.serviceUrl = serviceUrl;
        this.clientCredential = bifBCDNSClientCredential;
    }

    public VcApplyRespDto applyCrossChainCertificate(AbstractCrossChainCertificate certSigningRequest) {
        VcApplyReqDto vcApplyReqDto = new VcApplyReqDto();
        vcApplyReqDto.setContent(certSigningRequest.getEncodedToSign());
        vcApplyReqDto.setCredentialType(certSigningRequest.getType().ordinal());
        vcApplyReqDto.setPublicKey(
                Base64.encode(
                        certSigningRequest.getCredentialSubjectInstance().getSubjectPublicKey().getEncoded()
                )
        );
        vcApplyReqDto.setSign(
                clientCredential.signRequest(certSigningRequest.getEncodedToSign())
        );
        try (
                Response response = httpClient.newCall(
                        new Request.Builder()
                                .url(getRequestUrl(VC_APPLY_URL))
                                .post(RequestBody.create(JSON.toJSONString(vcApplyReqDto), JSON_MEDIA_TYPE))
                                .build()
                ).execute();
        ) {
            DataResp<VcApplyRespDto> resp = JSON.parseObject(
                    Objects.requireNonNull(response.body()).string(),
                    new TypeReference<DataResp<VcApplyRespDto>>() {}
            );
            if (resp.getErrorCode() != 0) {
                throw new RuntimeException(
                        StrUtil.format(
                                "resp with error ( code: {}, msg: {} )",
                                resp.getErrorCode(),
                                resp.getMessage()
                        )
                );
            }
            return Assert.notNull(resp.getData());
        } catch (Exception e) {
            throw new RuntimeException(
                    StrUtil.format(
                            "failed to call BIF BCDNS for {} : ",
                            VC_APPLY_URL
                    ),
                    e
            );
        }
    }

    private static TrustManager[] buildTrustManagers() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };
    }

    private String getRequestUrl(String req) {
        return StrUtil.endWith(serviceUrl, "/") ? StrUtil.replaceLast(serviceUrl, "/", "") : serviceUrl;
    }
}
